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
import android.os.Bundle;
import android.widget.TextView;

import com.android.systemui.plugins.LauncherOverlayPlugin;
import com.android.systemui.plugins.PluginLifecycleManager;
import com.android.systemui.plugins.PluginListener;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 插件宿主演示 Activity。注册 {@link LauncherOverlayPlugin} 监听并展示已连接插件数量。
 *
 * <p>完整演示插件生命周期：
 *   onPluginAttached -> onPluginLoaded -> (onPluginUnloaded) -> onPluginDetached
 */
public class PluginHostDemoActivity extends Activity implements PluginListener<LauncherOverlayPlugin> {

    private final AtomicInteger mPluginCount = new AtomicInteger(0);
    private TextView mStatusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setPadding(48, 48, 48, 48);
        setContentView(tv);
        mStatusText = tv;
        updateStatus("Registering plugin listener...");

        LauncherPluginHostApp.getPluginManager().addPluginListener(this, LauncherOverlayPlugin.class);
    }

    @Override
    protected void onDestroy() {
        LauncherPluginHostApp.getPluginManager().removePluginListener(this);
        super.onDestroy();
    }

    @Override
    public boolean onPluginAttached(PluginLifecycleManager<LauncherOverlayPlugin> manager) {
        mPluginCount.incrementAndGet();
        updateStatus("Plugin attached: " + manager.getComponentName());
        return true;
    }

    @Override
    public void onPluginLoaded(
            LauncherOverlayPlugin plugin, Context pluginContext,
            PluginLifecycleManager<LauncherOverlayPlugin> manager) {
        updateStatus("Plugin loaded: " + manager.getComponentName()
                + " (" + mPluginCount.get() + " connected)\n"
                + "Overlay manager: " + plugin.createOverlayManager(this).getClass().getName());
    }

    @Override
    public void onPluginUnloaded(
            LauncherOverlayPlugin plugin, PluginLifecycleManager<LauncherOverlayPlugin> manager) {
        mPluginCount.decrementAndGet();
        updateStatus("Plugin unloaded: " + manager.getComponentName());
    }

    @Override
    public void onPluginDetached(PluginLifecycleManager<LauncherOverlayPlugin> manager) {
        updateStatus("Plugin detached: " + manager.getComponentName());
    }

    private void updateStatus(String msg) {
        if (mStatusText != null) {
            mStatusText.setText("Plugin Host Demo\n\n" + msg);
        }
    }
}
