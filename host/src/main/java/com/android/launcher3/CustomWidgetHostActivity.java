/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.launcher3;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.systemui.plugins.PluginLifecycleManager;
import com.android.systemui.plugins.PluginListener;
import com.android.systemui.plugins.WidgetViewPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 演示"插件提供复杂自定义 View"场景的宿主 Activity。
 *
 * <p>注册多个 {@link WidgetViewPlugin} 监听（不同 action），插件加载后调用其
 * {@link WidgetViewPlugin#createView(Context)}，把返回的卡片式 View attach 到本 Activity
 * 的 {@link LinearLayout} 容器中，垂直堆叠、水平居中。
 *
 * <p>不同 action 的插件互不干扰，可同时加载、独立卸载。
 */
public class CustomWidgetHostActivity extends Activity {

    /** 媒体卡片插件 action（example-plugin）。 */
    private static final String ACTION_WIDGET_VIEW_MEDIA =
            "com.android.systemui.action.PLUGIN_WIDGET_VIEW";
    /** 天气卡片插件 action（weather-plugin）。 */
    private static final String ACTION_WIDGET_VIEW_WEATHER =
            "com.android.systemui.action.PLUGIN_WIDGET_VIEW_WEATHER";

    private LinearLayout mContainer;
    private TextView mStatusText;

    /** 每个 action 对应一个独立监听器 + 已加载视图集合，按 action 隔离。 */
    private final Map<String, PluginListener<WidgetViewPlugin>> mListeners = new LinkedHashMap<>();
    /** 已加载视图：key = action + ComponentName，value = attach 的 View。 */
    private final Map<String, View> mPluginViews = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 顶部标题
        TextView title = new TextView(this);
        title.setText("Plugin Cards");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 48, 0, 24);

        // 卡片容器：水平并排、整体居中
        mContainer = new LinearLayout(this);
        mContainer.setOrientation(LinearLayout.HORIZONTAL);
        mContainer.setGravity(Gravity.CENTER);
        mContainer.setPadding(0, 0, 0, 0);

        // 底部状态栏：半透明胶囊样式
        mStatusText = new TextView(this);
        mStatusText.setGravity(Gravity.CENTER);
        mStatusText.setTextColor(Color.rgb(180, 200, 220));
        mStatusText.setTextSize(12);
        mStatusText.setPadding(48, 20, 48, 20);
        mStatusText.setText("Searching for widget plugins...");
        android.graphics.drawable.GradientDrawable statusBg =
                new android.graphics.drawable.GradientDrawable();
        statusBg.setColor(Color.argb(180, 30, 38, 52));
        statusBg.setCornerRadius(24);
        mStatusText.setBackground(statusBg);
        mStatusText.setTranslationY(-24);

        // 根容器：深色径向渐变背景
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable rootBg =
                new android.graphics.drawable.GradientDrawable(
                        android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{Color.rgb(14, 18, 28), Color.rgb(24, 32, 46)});
        root.setBackground(rootBg);

        LinearLayout.LayoutParams titleLp =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        root.addView(title, titleLp);

        LinearLayout.LayoutParams containerLp =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1);
        root.addView(mContainer, containerLp);

        LinearLayout.LayoutParams statusLp =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        statusLp.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(mStatusText, statusLp);

        setContentView(root);

        // 媒体卡片监听
        PluginListener<WidgetViewPlugin> mediaListener = createListener(ACTION_WIDGET_VIEW_MEDIA);
        mListeners.put(ACTION_WIDGET_VIEW_MEDIA, mediaListener);
        LauncherPluginHostApp.getPluginManager()
                .addPluginListener(ACTION_WIDGET_VIEW_MEDIA, mediaListener,
                        WidgetViewPlugin.class, false);

        // 天气卡片监听
        PluginListener<WidgetViewPlugin> weatherListener =
                createListener(ACTION_WIDGET_VIEW_WEATHER);
        mListeners.put(ACTION_WIDGET_VIEW_WEATHER, weatherListener);
        LauncherPluginHostApp.getPluginManager()
                .addPluginListener(ACTION_WIDGET_VIEW_WEATHER, weatherListener,
                        WidgetViewPlugin.class, false);
    }

    /** 为指定 action 构造一个 PluginListener，隔离 view 生命周期。 */
    private PluginListener<WidgetViewPlugin> createListener(String action) {
        return new PluginListener<WidgetViewPlugin>() {
            @Override
            public boolean onPluginAttached(PluginLifecycleManager<WidgetViewPlugin> manager) {
                updateStatus();
                return true;
            }

            @Override
            public void onPluginLoaded(
                    WidgetViewPlugin plugin,
                    Context pluginContext,
                    PluginLifecycleManager<WidgetViewPlugin> manager) {
                View pluginView = plugin.createView(pluginContext);
                if (pluginView == null) {
                    return;
                }
                String key = action + "/" + manager.getComponentName().flattenToShortString();
                // 移除同 key 旧实例（升级场景）
                View old = mPluginViews.remove(key);
                if (old != null) mContainer.removeView(old);
                mPluginViews.put(key, pluginView);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMarginEnd(24);
                mContainer.addView(pluginView, lp);
                updateStatus();
            }

            @Override
            public void onPluginUnloaded(
                    WidgetViewPlugin plugin,
                    PluginLifecycleManager<WidgetViewPlugin> manager) {
                String key = action + "/" + manager.getComponentName().flattenToShortString();
                View v = mPluginViews.remove(key);
                if (v != null) mContainer.removeView(v);
                updateStatus();
            }

            @Override
            public void onPluginDetached(PluginLifecycleManager<WidgetViewPlugin> manager) {
                String key = action + "/" + manager.getComponentName().flattenToShortString();
                View v = mPluginViews.remove(key);
                if (v != null) mContainer.removeView(v);
                updateStatus();
            }
        };
    }

    @Override
    protected void onDestroy() {
        for (PluginListener<WidgetViewPlugin> l : mListeners.values()) {
            LauncherPluginHostApp.getPluginManager().removePluginListener(l);
        }
        mListeners.clear();
        mPluginViews.clear();
        super.onDestroy();
    }

    private void updateStatus() {
        if (mStatusText == null) return;
        StringBuilder sb = new StringBuilder("Custom Widget Host\n\n");
        if (mPluginViews.isEmpty()) {
            sb.append("No widget plugins attached.");
        } else {
            sb.append("Attached plugins (").append(mPluginViews.size()).append("):");
            for (String key : mPluginViews.keySet()) {
                sb.append("\n  • ").append(key);
            }
        }
        mStatusText.setText(sb.toString());
    }
}
