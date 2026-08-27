/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.shared.plugins;

import android.app.Notification;
import android.app.Notification.Action;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.messages.nano.SystemMessageProto.SystemMessage;
import com.android.systemui.plugins.Plugin;
import com.android.systemui.plugins.PluginListener;
import com.android.systemui.plugins.PluginManager;
import com.android.systemui.shared.plugins.VersionInfo.InvalidVersionException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * 协调某一 action 下所有可用插件。
 *
 * <p>通过 {@link PackageManager} 以 {@link Intent} action 查询可用插件。
 *
 * @param <T> 管理的插件类型。
 */
public class PluginActionManager<T extends Plugin> {

    private static final boolean DEBUG = false;

    private static final String TAG = "PluginActionManager";
    public static final String PLUGIN_PERMISSION = "com.android.systemui.permission.PLUGIN";

    private final Context mContext;
    private final PluginListener<T> mListener;
    private final String mAction;
    private final boolean mAllowMultiple;
    private final NotificationManager mNotificationManager;
    private final PluginEnabler mPluginEnabler;
    private final PluginInstance.Factory mPluginInstanceFactory;
    private final ArraySet<String> mPrivilegedPlugins = new ArraySet<>();

    @VisibleForTesting
    private final ArrayList<PluginInstance<T>> mPluginInstances = new ArrayList<>();
    private final boolean mIsDebuggable;
    private final PackageManager mPm;
    private final Class<T> mPluginClass;
    private final Executor mMainExecutor;
    private final Executor mBgExecutor;

    private PluginActionManager(
            Context context,
            PackageManager pm,
            String action,
            PluginListener<T> listener,
            Class<T> pluginClass,
            boolean allowMultiple,
            Executor mainExecutor,
            Executor bgExecutor,
            boolean debuggable,
            NotificationManager notificationManager,
            PluginEnabler pluginEnabler,
            List<String> privilegedPlugins,
            PluginInstance.Factory pluginInstanceFactory) {
        mPluginClass = pluginClass;
        mMainExecutor = mainExecutor;
        mBgExecutor = bgExecutor;
        mContext = context;
        mPm = pm;
        mAction = action;
        mListener = listener;
        mAllowMultiple = allowMultiple;
        mNotificationManager = notificationManager;
        mPluginEnabler = pluginEnabler;
        mPluginInstanceFactory = pluginInstanceFactory;
        mPrivilegedPlugins.addAll(privilegedPlugins);
        mIsDebuggable = debuggable;
    }

    /** 加载匹配本实例 action 的全部插件。 */
    public void loadAll() {
        if (DEBUG) Log.d(TAG, "startListening");
        mBgExecutor.execute(() -> queryAll());
    }

    /** 卸载本实例管理的全部插件。 */
    public void destroy() {
        if (DEBUG) Log.d(TAG, "stopListening");
        ArrayList<PluginInstance<T>> plugins = new ArrayList<>(mPluginInstances);
        for (PluginInstance<T> plugInstance : plugins) {
            mMainExecutor.execute(() -> onPluginDisconnected(plugInstance));
        }
    }

    /** 卸载本实例管理的全部匹配插件。 */
    public void onPackageRemoved(String pkg) {
        mBgExecutor.execute(() -> removePkg(pkg));
    }

    /** 先卸载再重新加载本实例管理的全部匹配插件。 */
    public void reloadPackage(String pkg) {
        mBgExecutor.execute(() -> {
            removePkg(pkg);
            queryPkg(pkg);
        });
    }

    /** 禁用本实例管理的某个指定插件。 */
    public boolean checkAndDisable(String className) {
        boolean disableAny = false;
        ArrayList<PluginInstance<T>> plugins = new ArrayList<>(mPluginInstances);
        for (PluginInstance<T> info : plugins) {
            if (className.startsWith(info.getPackage())) {
                disableAny |= disable(info, PluginEnabler.DISABLED_FROM_EXPLICIT_CRASH);
            }
        }
        return disableAny;
    }

    /** 禁用本实例管理的全部插件。 */
    public boolean disableAll() {
        ArrayList<PluginInstance<T>> plugins = new ArrayList<>(mPluginInstances);
        boolean disabledAny = false;
        for (int i = 0; i < plugins.size(); i++) {
            disabledAny |= disable(plugins.get(i), PluginEnabler.DISABLED_FROM_SYSTEM_CRASH);
        }
        return disabledAny;
    }

    boolean isPluginPrivileged(ComponentName pluginName) {
        for (String componentNameOrPackage : mPrivilegedPlugins) {
            ComponentName componentName = ComponentName.unflattenFromString(componentNameOrPackage);
            if (componentName == null) {
                if (componentNameOrPackage.equals(pluginName.getPackageName())) {
                    return true;
                }
            } else {
                if (componentName.equals(pluginName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean disable(
            PluginInstance<T> pluginInstance, @PluginEnabler.DisableReason int reason) {
        // 简单粗暴原则：行为异常的插件直接禁用，直到卸载/重装后才会恢复。

        ComponentName pluginComponent = pluginInstance.getComponentName();
        // 若在崩溃堆栈中检测到某个插件则只禁用它；若无法定位肇事插件，则全部禁用
        // （默认其中必有一个是罪魁祸首）。
        if (isPluginPrivileged(pluginComponent)) {
            // 特权插件属于 OS 一部分，不禁用。
            return false;
        }
        Log.w(TAG, "Disabling plugin " + pluginComponent.flattenToShortString());
        mPluginEnabler.setDisabled(pluginComponent, reason);

        return true;
    }

    <C> boolean dependsOn(Plugin p, Class<C> cls) {
        ArrayList<PluginInstance<T>> instances = new ArrayList<>(mPluginInstances);
        for (PluginInstance<T> instance : instances) {
            if (instance.containsPluginClass(p.getClass())) {
                return instance.getVersionInfo() != null && instance.getVersionInfo().hasClass(cls);
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s@%s (action=%s)",
                getClass().getSimpleName(), hashCode(), mAction);
    }

    private void onPluginConnected(PluginInstance<T> pluginInstance) {
        if (DEBUG) Log.d(TAG, "onPluginConnected");
        PluginPrefs.setHasPlugins(mContext);
        pluginInstance.onCreate();
    }

    private void onPluginDisconnected(PluginInstance<T> pluginInstance) {
        if (DEBUG) Log.d(TAG, "onPluginDisconnected");
        pluginInstance.onDestroy();
    }

    private void queryAll() {
        if (DEBUG) Log.d(TAG, "queryAll " + mAction);
        for (int i = mPluginInstances.size() - 1; i >= 0; i--) {
            PluginInstance<T> pluginInstance = mPluginInstances.get(i);
            mMainExecutor.execute(() -> onPluginDisconnected(pluginInstance));
        }
        mPluginInstances.clear();
        handleQueryPlugins(null);
    }

    private void removePkg(String pkg) {
        for (int i = mPluginInstances.size() - 1; i >= 0; i--) {
            final PluginInstance<T> pluginInstance = mPluginInstances.get(i);
            if (pluginInstance.getPackage().equals(pkg)) {
                mMainExecutor.execute(() -> onPluginDisconnected(pluginInstance));
                mPluginInstances.remove(i);
            }
        }
    }

    private void queryPkg(String pkg) {
        if (DEBUG) Log.d(TAG, "queryPkg " + pkg + " " + mAction);
        if (mAllowMultiple || (mPluginInstances.size() == 0)) {
            handleQueryPlugins(pkg);
        } else {
            if (DEBUG) Log.d(TAG, "Too many of " + mAction);
        }
    }

    private void handleQueryPlugins(String pkgName) {
        // 这里并不真正启动 service，只是借用 PackageManager 查询 service 声明的便捷机制
        // 来管理插件。
        Intent intent = new Intent(mAction);
        if (pkgName != null) {
            intent.setPackage(pkgName);
        }
        List<ResolveInfo> result = mPm.queryIntentServices(intent, 0);
        if (DEBUG) {
            Log.d(TAG, "Found " + result.size() + " plugins");
            for (ResolveInfo info : result) {
                ComponentName name = new ComponentName(info.serviceInfo.packageName,
                        info.serviceInfo.name);
                Log.d(TAG, "  " + name);
            }
        }

        if (result.size() > 1 && !mAllowMultiple) {
            // TODO: 弹出警告提示多个插件冲突。
            Log.w(TAG, "Multiple plugins found for " + mAction);
            return;
        }
        for (ResolveInfo info : result) {
            ComponentName name = new ComponentName(info.serviceInfo.packageName,
                    info.serviceInfo.name);
            PluginInstance<T> pluginInstance = loadPluginComponent(name);
            if (pluginInstance != null) {
                // 先加入插件列表，再发送 PLUGIN_CONNECTED 消息
                mPluginInstances.add(pluginInstance);
                mMainExecutor.execute(() -> onPluginConnected(pluginInstance));
            }
        }
    }

    private PluginInstance<T> loadPluginComponent(ComponentName component) {
        // 之前已校验过，这里再校验一次，确保生产版本绝不使用这些原型插件。
        if (!mIsDebuggable && !isPluginPrivileged(component)) {
            // 生产构建绝不允许加载，仅用于原型验证。
            Log.w(TAG, "Plugin cannot be loaded on production build: " + component);
            return null;
        }
        if (!mPluginEnabler.isEnabled(component)) {
            if (DEBUG) {
                Log.d(TAG, "Plugin is not enabled, aborting load: " + component);
            }
            return null;
        }
        String packageName = component.getPackageName();
        try {
            // TODO: 未开启 IGNORE_SECURITY 时这步可能并非必需。
            if (mPm.checkPermission(PLUGIN_PERMISSION, packageName)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Plugin doesn't have permission: " + packageName);
                return null;
            }

            ApplicationInfo appInfo = mPm.getApplicationInfo(packageName, 0);
            // TODO: 仅在需要旧版版本校验时才在版本检查前创建插件。
            if (DEBUG) {
                Log.d(TAG, "createPlugin: " + component);
            }
            try {
                return mPluginInstanceFactory.create(
                        mContext, appInfo, component,
                        mPluginClass, mListener);
            } catch (InvalidVersionException e) {
                reportInvalidVersion(component, component.getClassName(), e);
            }
        } catch (Throwable e) {
            Log.w(TAG, "Couldn't load plugin: " + component, e);
            return null;
        }

        return null;
    }

    private void reportInvalidVersion(
            ComponentName component, String className, InvalidVersionException e) {
        final int icon = Resources.getSystem().getIdentifier(
                "stat_sys_warning", "drawable", "android");
        final int color = Resources.getSystem().getIdentifier(
                "system_notification_accent_color", "color", "android");
        final Notification.Builder nb = new Notification.Builder(mContext,
                PluginManager.NOTIFICATION_CHANNEL_ID)
                .setStyle(new Notification.BigTextStyle())
                .setSmallIcon(icon)
                .setWhen(0)
                .setShowWhen(false)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setColor(mContext.getColor(color));
        String label = className;
        try {
            label = mPm.getServiceInfo(component, 0).loadLabel(mPm).toString();
        } catch (NameNotFoundException e2) {
            // 忽略：插件标签获取失败时直接使用类名
        }
        if (!e.isTooNew()) {
            // 无需本地化：该提示永远不会出现在用户正式版本中
            nb.setContentTitle("Plugin \"" + label + "\" is too old")
                    .setContentText("Contact plugin developer to get an updated"
                            + " version.\n" + e.getMessage());
        } else {
            // 无需本地化：该提示永远不会出现在用户正式版本中
            nb.setContentTitle("Plugin \"" + label + "\" is too new")
                    .setContentText("Check to see if an OTA is available.\n"
                            + e.getMessage());
        }
        Intent i = new Intent(PluginManagerImpl.DISABLE_PLUGIN).setData(
                Uri.parse("package://" + component.flattenToString()));
        PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, i,
                PendingIntent.FLAG_IMMUTABLE);
        nb.addAction(new Action.Builder(null, "Disable plugin", pi).build());
        mNotificationManager.notify(SystemMessage.NOTE_PLUGIN, nb.build());
        // TODO: 向用户提示插件加载失败。
        Log.w(TAG, "Error loading plugin; " + e.getMessage());
    }

    /**
     * 构造 {@link PluginActionManager}
     */
    public static class Factory {
        private final Context mContext;
        private final PackageManager mPackageManager;
        private final Executor mMainExecutor;
        private final Executor mBgExecutor;
        private final NotificationManager mNotificationManager;
        private final PluginEnabler mPluginEnabler;
        private final List<String> mPrivilegedPlugins;
        private final PluginInstance.Factory mPluginInstanceFactory;

        public Factory(Context context, PackageManager packageManager,
                Executor mainExecutor, Executor bgExecutor,
                NotificationManager notificationManager, PluginEnabler pluginEnabler,
                List<String> privilegedPlugins, PluginInstance.Factory pluginInstanceFactory) {
            mContext = context;
            mPackageManager = packageManager;
            mMainExecutor = mainExecutor;
            mBgExecutor = bgExecutor;
            mNotificationManager = notificationManager;
            mPluginEnabler = pluginEnabler;
            mPrivilegedPlugins = privilegedPlugins;
            mPluginInstanceFactory = pluginInstanceFactory;
        }

        <T extends Plugin> PluginActionManager<T> create(
                String action, PluginListener<T> listener, Class<T> pluginClass,
                boolean allowMultiple, boolean debuggable) {
            return new PluginActionManager<>(mContext, mPackageManager, action, listener,
                    pluginClass, allowMultiple, mMainExecutor, mBgExecutor,
                    debuggable, mNotificationManager, mPluginEnabler,
                    mPrivilegedPlugins, mPluginInstanceFactory);
        }
    }

    /** 用插件 ClassLoader 包装上下文，使插件可加载自己的资源。 */
    public static class PluginContextWrapper extends ContextWrapper {
        private final ClassLoader mClassLoader;
        private LayoutInflater mInflater;

        public PluginContextWrapper(Context base, ClassLoader classLoader) {
            super(base);
            mClassLoader = classLoader;
        }

        @Override
        public ClassLoader getClassLoader() {
            return mClassLoader;
        }

        @Override
        public Object getSystemService(String name) {
            if (LAYOUT_INFLATER_SERVICE.equals(name)) {
                if (mInflater == null) {
                    mInflater = LayoutInflater.from(getBaseContext()).cloneInContext(this);
                }
                return mInflater;
            }
            return getBaseContext().getSystemService(name);
        }
    }

}
