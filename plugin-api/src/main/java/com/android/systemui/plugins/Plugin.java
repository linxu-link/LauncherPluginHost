/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.systemui.plugins;

import android.content.Context;

import com.android.systemui.plugins.annotations.Requires;

/**
 * 插件是独立的 APK，期望实现由 SystemUI 提供的接口。它们的代码会被
 * 动态加载到 SysUI 进程中，从而允许在单个 Android 构建上创建并运行多个原型。
 *
 * PluginLifecycle（插件生命周期）：
 * <pre class="prettyprint">
 *
 * plugin.onCreate(Context sysuiContext, Context pluginContext);
 * --- 这总是会在任何其他调用之前被调用
 *
 * pluginListener.onPluginConnected(Plugin p);
 * --- 这会让插件钩子（plugin hook）知道某个插件现已连接。
 *
 * ** 在 sysui 与 plugin 之间的任何其他来回调用 **
 *
 * pluginListener.onPluginDisconnected(Plugin p);
 * --- 让插件钩子知道它应当停止与该插件交互，并释放对它的所有引用。
 *
 * plugin.onDestroy();
 * --- 最后，插件可以执行任何清理工作，以确保它不会泄漏到 SysUI 进程中。
 *
 * 每当插件 APK 被更新时，插件都会被销毁并重新创建，以加载新的代码/资源。
 *
 * </pre>
 *
 * 创建插件钩子（plugin hook）：
 *
 * 要创建插件钩子，首先在 frameworks/base/packages/SystemUI/plugin 中
 * 创建一个继承 Plugin 的接口。在接口中包含任何你希望从 sysui 调用的钩子，
 * 并为任何需要传递到插件中的内容创建回调接口。
 *
 * 然后，要挂载任何插件，只需添加一个插件监听器，每当有新插件被安装、
 * 更新或启用时，onPluginConnected 就会被调用。
 *
 * 插件是通过查询服务来发现的，因此要让 SysUI 知道它的存在，请创建一个
 * 名称指向你的插件接口实现、并带有相应 action 的服务。
 */
public interface Plugin {

    /**
     * @deprecated
     * @see Requires
     */
    @Deprecated
    default int getVersion() {
        // 默认值 -1 表示该插件支持新的 Requires 模型。
        return -1;
    }

    default void onCreate(Context sysuiContext, Context pluginContext) {
    }

    default void onDestroy() {
    }
}
