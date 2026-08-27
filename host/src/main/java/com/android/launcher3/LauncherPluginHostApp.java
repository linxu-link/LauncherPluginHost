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

import android.app.Application;

import com.android.launcher3.uioverrides.plugins.PluginManagerWrapperImpl;
import com.android.launcher3.util.PluginManagerWrapper;

/**
 * 插件宿主 Application 入口。
 *
 * <p>在 onCreate 时提前初始化插件框架单例，后续 Activity / Widget 注册的插件监听可挂在
 * 共享的 PluginManagerImpl 上。
 */
public class LauncherPluginHostApp extends Application {

    private static PluginManagerWrapperImpl sPluginManager;

    @Override
    public void onCreate() {
        super.onCreate();
        sPluginManager = new PluginManagerWrapperImpl(this);
    }

    /** 获取全局插件管理器实现。 */
    public static PluginManagerWrapperImpl getPluginManager() {
        return sPluginManager;
    }

    /** 与 Launcher3 的 PluginManagerWrapper.INSTANCE 风格对齐的静态访问器。 */
    public static PluginManagerWrapper getPluginManagerWrapper() {
        return sPluginManager;
    }
}
