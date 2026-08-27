/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.app.LoadedApk;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.plugins.Plugin;
import com.android.systemui.plugins.PluginFragment;
import com.android.systemui.plugins.PluginLifecycleManager;
import com.android.systemui.plugins.PluginListener;
import com.android.systemui.plugins.PluginProtector;
import com.android.systemui.plugins.PluginWrapper;
import com.android.systemui.plugins.ProtectedPluginListener;

import dalvik.system.PathClassLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * 保存单个插件实例。
 *
 * <p>本类及其 Factory 负责真正实例化插件，并管理与之相关的所有状态。
 *
 * @param <T> 所包含的插件类型。
 */
public class PluginInstance<T extends Plugin>
        implements PluginLifecycleManager, ProtectedPluginListener {
    private static final String TAG = "PluginInstance";

    private final Context mAppContext;
    private final PluginListener<T> mListener;
    private final ComponentName mComponentName;
    private final PluginFactory<T> mPluginFactory;
    private final String mTag;

    private boolean mHasError = false;
    private BiConsumer<String, String> mLogConsumer = null;
    private Context mPluginContext;
    private T mPlugin;

    /** 构造插件实例。 */
    public PluginInstance(
            Context appContext,
            PluginListener<T> listener,
            ComponentName componentName,
            PluginFactory<T> pluginFactory,
            T plugin) {
        mAppContext = appContext;
        mListener = listener;
        mComponentName = componentName;
        mPluginFactory = pluginFactory;
        mPlugin = plugin;
        mTag = TAG + "[" + mComponentName.getShortClassName() + "]"
                + '@' + Integer.toHexString(hashCode());

        if (mPlugin != null) {
            mPluginContext = mPluginFactory.createPluginContext();
        }
    }

    @Override
    public String toString() {
        return mTag;
    }

    /** 插件是否发生过异常。 */
    public boolean hasError() {
        return mHasError;
    }

    public void setLogFunc(BiConsumer logConsumer) {
        mLogConsumer = logConsumer;
    }

    private void log(String message) {
        if (mLogConsumer != null) {
            mLogConsumer.accept(mTag, message);
        }
    }

    @Override
    public synchronized boolean onFail(String className, String methodName, Throwable failure) {
        Log.e(TAG, "Failure from " + mPlugin + ". Disabling Plugin.");
        mHasError = true;
        unloadPlugin();
        mListener.onPluginDetached(this);
        return true;
    }

    /** 通知监听器与插件：插件已创建。 */
    public synchronized void onCreate() {
        if (mHasError) {
            log("Previous Fatal Exception detected for plugin class");
            return;
        }

        boolean loadPlugin = mListener.onPluginAttached(this);
        if (!loadPlugin) {
            if (mPlugin != null) {
                log("onCreate: auto-unload");
                unloadPlugin();
            }
            return;
        }

        if (mPlugin == null) {
            log("onCreate: auto-load");
            loadPlugin();
            return;
        }

        if (!checkVersion()) {
            log("onCreate: version check failed");
            return;
        }

        log("onCreate: load callbacks");
        if (!(mPlugin instanceof PluginFragment)) {
            // 仅对非 Fragment 插件调用 onCreate，Fragment 插件的 onCreate 由
            // Fragment 自身生命周期驱动。
            mPlugin.onCreate(mAppContext, mPluginContext);
        }
        mListener.onPluginLoaded(mPlugin, mPluginContext, this);
    }

    /** 通知监听器与插件：插件正在被关闭。 */
    public synchronized void onDestroy() {
        if (mHasError) {
            // 出错插件已在错误处理器中卸载
            log("onDestroy - no-op");
            return;
        }

        log("onDestroy");
        unloadPlugin();
        mListener.onPluginDetached(this);
    }

    /** 返回当前插件实例（若已加载）。 */
    public T getPlugin() {
        return mHasError ? null : mPlugin;
    }

    /**
     * 若插件尚未加载，则加载并创建插件实例。
     */
    public synchronized void loadPlugin() {
        if (mHasError) {
            log("Previous Fatal Exception detected for plugin class");
            return;
        }

        if (mPlugin != null) {
            log("Load request when already loaded");
            return;
        }

        // 测试运行中这两次调用约耗时 1 - 1.5 秒
        mPlugin = mPluginFactory.createPlugin(this);
        mPluginContext = mPluginFactory.createPluginContext();
        if (mPlugin == null || mPluginContext == null) {
            Log.e(mTag, "Requested load, but failed");
            return;
        }

        if (!checkVersion()) {
            log("loadPlugin: version check failed");
            return;
        }

        log("Loaded plugin; running callbacks");
        if (!(mPlugin instanceof PluginFragment)) {
            // 仅对非 Fragment 插件调用 onCreate，Fragment 插件的 onCreate 由
            // Fragment 自身生命周期驱动。
            mPlugin.onCreate(mAppContext, mPluginContext);
        }
        mListener.onPluginLoaded(mPlugin, mPluginContext, this);
    }

    /**
     * 检查插件版本，校验失败时永久销毁插件实例
     */
    private synchronized boolean checkVersion() {
        if (mHasError) {
            return false;
        }

        if (mPlugin == null) {
            return true;
        }

        if (mPluginFactory.checkVersion(mPlugin)) {
            return true;
        }

        Log.wtf(TAG, "Version check failed for " + mPlugin.getClass().getSimpleName());
        mHasError = true;
        unloadPlugin();
        mListener.onPluginDetached(this);
        return false;
    }

    /**
     * 卸载并销毁当前插件实例（若存在）。
     *
     * <p>若无其他引用，将释放对应内存。
     */
    public synchronized void unloadPlugin() {
        if (mPlugin == null) {
            log("Unload request when already unloaded");
            return;
        }

        log("Unloading plugin, running callbacks");
        mListener.onPluginUnloaded(mPlugin, this);
        if (!(mPlugin instanceof PluginFragment)) {
            // 仅对非 Fragment 插件调用 onDestroy，Fragment 插件的 onDestroy 由
            // Fragment 自身生命周期驱动。
            mPlugin.onDestroy();
        }
        mPlugin = null;
        mPluginContext = null;
    }

    /**
     * 返回所包含的插件是否与传入的类名匹配。
     *
     * <p>通过类名字符串比较实现。
     **/
    public boolean containsPluginClass(Class pluginClass) {
        return mComponentName.getClassName().equals(pluginClass.getName());
    }

    public ComponentName getComponentName() {
        return mComponentName;
    }

    public String getPackage() {
        return mComponentName.getPackageName();
    }

    public VersionInfo getVersionInfo() {
        return mPluginFactory.getVersionInfo(mPlugin);
    }

    @VisibleForTesting
    Context getPluginContext() {
        return mPluginContext;
    }

    /** 用于创建新的 {@link PluginInstance}。 */
    public static class Factory {
        private final ClassLoader mBaseClassLoader;
        private final InstanceFactory<?> mInstanceFactory;
        private final VersionChecker mVersionChecker;
        private final boolean mIsDebug;
        private final List<String> mPrivilegedPlugins;
        // 缓存插件包名 -> ClassLoader，保证同一插件类始终由同一 ClassLoader 加载
        // （否则插件与插件 Context 各自创建的插件私有类会成为不同的类，强制转换会失败）。
        private final Map<String, ClassLoader> mClassLoaders = new ArrayMap<>();

        /** 用于构造 {@link PluginInstance} 的工厂。 */
        public Factory(ClassLoader classLoader, InstanceFactory<?> instanceFactory,
                VersionChecker versionChecker,
                List<String> privilegedPlugins,
                boolean isDebug) {
            mPrivilegedPlugins = privilegedPlugins;
            mBaseClassLoader = classLoader;
            mInstanceFactory = instanceFactory;
            mVersionChecker = versionChecker;
            mIsDebug = isDebug;
        }

        /**
         * 移除并返回指定包名缓存的 ClassLoader（若有）。插件包升级时调用，
         * 使下次加载时创建全新的加载器。
         */
        public ClassLoader clearClassLoader(String packageName) {
            return mClassLoaders.remove(packageName);
        }

        /** 构造一个新的 PluginInstance。 */
        public <T extends Plugin> PluginInstance<T> create(
                Context context,
                ApplicationInfo appInfo,
                ComponentName componentName,
                Class<T> pluginClass,
                PluginListener<T> listener)
                throws PackageManager.NameNotFoundException, ClassNotFoundException,
                InstantiationException, IllegalAccessException {

            PluginFactory<T> pluginFactory = new PluginFactory<T>(
                    context, mInstanceFactory, appInfo, componentName, mVersionChecker, pluginClass,
                    () -> getClassLoader(appInfo, mBaseClassLoader));
            return new PluginInstance<T>(
                    context, listener, componentName, pluginFactory, null);
        }

        private boolean isPluginPackagePrivileged(String packageName) {
            for (String componentNameOrPackage : mPrivilegedPlugins) {
                ComponentName componentName = ComponentName.unflattenFromString(
                        componentNameOrPackage);
                if (componentName != null) {
                    if (componentName.getPackageName().equals(packageName)) {
                        return true;
                    }
                } else if (componentNameOrPackage.equals(packageName)) {
                    return true;
                }
            }
            return false;
        }

        private ClassLoader getParentClassLoader(ClassLoader baseClassLoader) {
            return new PluginManagerImpl.ClassLoaderFilter(
                    baseClassLoader,
                    "androidx.constraintlayout.widget",
                    "com.android.systemui.common",
                    "com.android.systemui.log",
                    "com.android.systemui.plugin");
        }

        /** 返回指定插件专用的 ClassLoader。 */
        private ClassLoader getClassLoader(ApplicationInfo appInfo,
                ClassLoader baseClassLoader) {
            if (!mIsDebug && !isPluginPackagePrivileged(appInfo.packageName)) {
                Log.w(TAG, "Cannot get class loader for non-privileged plugin. Src:"
                        + appInfo.sourceDir + ", pkg: " + appInfo.packageName);
                return null;
            }
            synchronized (mClassLoaders) {
                ClassLoader cached = mClassLoaders.get(appInfo.packageName);
                if (cached != null) {
                    return cached;
                }
                List<String> zipPaths = new ArrayList<>();
                List<String> libPaths = new ArrayList<>();
                LoadedApk.makePaths(null, true, appInfo, zipPaths, libPaths);
                ClassLoader classLoader = new PathClassLoader(
                        TextUtils.join(File.pathSeparator, zipPaths),
                        TextUtils.join(File.pathSeparator, libPaths),
                        getParentClassLoader(baseClassLoader));
                mClassLoaders.put(appInfo.packageName, classLoader);
                return classLoader;
            }
        }
    }

    /** 比较插件类与实现类以进行版本匹配。 */
    public interface VersionChecker {
        /** 比较两个插件类，匹配返回 true。 */
        <T extends Plugin> boolean checkVersion(
                Class<T> instanceClass, Class<T> pluginClass, Plugin plugin);

        /** 返回目标类的 VersionInfo。 */
        <T extends Plugin> VersionInfo getVersionInfo(Class<T> instanceclass);
    }

    /** 比较插件类与实现类以进行版本匹配。 */
    public static class VersionCheckerImpl implements VersionChecker {
        @Override
        /** 比较两个插件类。 */
        public <T extends Plugin> boolean checkVersion(
                Class<T> instanceClass, Class<T> pluginClass, Plugin plugin) {
            VersionInfo pluginVersion = new VersionInfo().addClass(pluginClass);
            VersionInfo instanceVersion = new VersionInfo().addClass(instanceClass);
            if (instanceVersion.hasVersionInfo()) {
                pluginVersion.checkVersion(instanceVersion);
            } else if (plugin != null) {
                int fallbackVersion = plugin.getVersion();
                if (fallbackVersion != pluginVersion.getDefaultVersion()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        /** 返回该类的版本信息。 */
        public <T extends Plugin> VersionInfo getVersionInfo(Class<T> instanceClass) {
            VersionInfo instanceVersion = new VersionInfo().addClass(instanceClass);
            return instanceVersion.hasVersionInfo() ? instanceVersion : null;
        }
    }

    /**
     * 简单的新实例创建类，便于测试。
     *
     * @param <T> 创建的插件类型。
     **/
    public static class InstanceFactory<T extends Plugin> {
        T create(Class cls) throws IllegalAccessException, InstantiationException {
            return (T) cls.newInstance();
        }
    }

    /**
     * InstanceFactory 的实例化包装。
     *
     * @param <T> 待构造的插件对象类型
     **/
    public static class PluginFactory<T extends Plugin> {
        private final Context mContext;
        private final InstanceFactory<?> mInstanceFactory;
        private final ApplicationInfo mAppInfo;
        private final ComponentName mComponentName;
        private final VersionChecker mVersionChecker;
        private final Class<T> mPluginClass;
        private final Supplier<ClassLoader> mClassLoaderFactory;

        public PluginFactory(
                Context context,
                InstanceFactory<?> instanceFactory,
                ApplicationInfo appInfo,
                ComponentName componentName,
                VersionChecker versionChecker,
                Class<T> pluginClass,
                Supplier<ClassLoader> classLoaderFactory) {
            mContext = context;
            mInstanceFactory = instanceFactory;
            mAppInfo = appInfo;
            mComponentName = componentName;
            mVersionChecker = versionChecker;
            mPluginClass = pluginClass;
            mClassLoaderFactory = classLoaderFactory;
        }

        /** 通过工厂创建对应的插件对象 */
        public T createPlugin(ProtectedPluginListener listener) {
            try {
                ClassLoader loader = mClassLoaderFactory.get();
                Class<T> instanceClass = (Class<T>) Class.forName(
                        mComponentName.getClassName(), true, loader);
                T result = (T) mInstanceFactory.create(instanceClass);
                Log.v(TAG, "Created plugin: " + result);
                return PluginProtector.protectIfAble(result, listener);
            } catch (ReflectiveOperationException ex) {
                Log.wtf(TAG, "Failed to load plugin", ex);
            }
            return null;
        }

        /** 创建插件专用的 Context 包装器 */
        public Context createPluginContext() {
            try {
                ClassLoader loader = mClassLoaderFactory.get();
                return new PluginActionManager.PluginContextWrapper(
                    mContext.createApplicationContext(mAppInfo, 0), loader);
            } catch (NameNotFoundException ex) {
                Log.e(TAG, "Failed to create plugin context", ex);
            }
            return null;
        }

        /** 校验实例的版本 */
        public boolean checkVersion(T instance) {
            if (instance == null) {
                instance = createPlugin(null);
            }
            if (instance instanceof PluginWrapper) {
                instance = ((PluginWrapper<T>) instance).getPlugin();
            }
            return mVersionChecker.checkVersion(
                    (Class<T>) instance.getClass(), mPluginClass, instance);
        }

        /** 获取实例的版本信息 */
        public VersionInfo getVersionInfo(T instance) {
            if (instance == null) {
                instance = createPlugin(null);
            }
            if (instance instanceof PluginWrapper) {
                instance = ((PluginWrapper<T>) instance).getPlugin();
            }
            return mVersionChecker.getVersionInfo((Class<T>) instance.getClass());
        }
    }
}
