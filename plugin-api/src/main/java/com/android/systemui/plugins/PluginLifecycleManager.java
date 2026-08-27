/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.systemui.plugins;

import android.content.ComponentName;

import java.util.function.BiConsumer;

/**
 * 为使用者提供控制插件生命周期的能力。
 *
 * @param <T> 目标插件类型
 */
public interface PluginLifecycleManager<T extends Plugin> {
    /** 返回目标插件的 ComponentName。即使插件未加载也可以调用。 */
    ComponentName getComponentName();

    /** 返回目标插件的包名。即使插件未加载也可以调用。 */
    String getPackage();

    /** 返回当前已加载的插件实例（如果插件已加载） */
    T getPlugin();

    /** 日志标签和消息将被发送到所提供的 Consumer */
    void setLogFunc(BiConsumer<String, String> logConsumer);

    /** 如果插件当前已加载则返回 true */
    default boolean isLoaded() {
        return getPlugin() != null;
    }

    /**
     * 如果插件实例不存在，则加载并创建它。
     *
     * 如果插件实例此前不存在，这将使用新实例触发 {@link PluginListener#onPluginLoaded}。
     */
    void loadPlugin();

    /**
     * 如果插件实例存在，则卸载并销毁它。
     *
     * 如果在调用时确实存在具体的插件实例，这将触发 {@link PluginListener#onPluginUnloaded}。
     */
    void unloadPlugin();
}
