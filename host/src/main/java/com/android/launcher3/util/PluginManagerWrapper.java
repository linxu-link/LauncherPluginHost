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
package com.android.launcher3.util;

import com.android.systemui.plugins.Plugin;
import com.android.systemui.plugins.PluginListener;

import java.io.PrintWriter;

public class PluginManagerWrapper {

    public <T extends Plugin> void addPluginListener(
            PluginListener<T> listener, Class<T> pluginClass) {
        addPluginListener(listener, pluginClass, false);
    }

    public <T extends Plugin> void addPluginListener(
            PluginListener<T> listener, Class<T> pluginClass, boolean allowMultiple) {
    }

    /** 按 action 注册插件监听，可指定是否允许多个插件实例。 */
    public <T extends Plugin> void addPluginListener(
            String action, PluginListener<T> listener, Class<T> pluginClass,
            boolean allowMultiple) {
    }

    public void removePluginListener(PluginListener<? extends Plugin> listener) { }

    public void dump(PrintWriter pw) { }
}
