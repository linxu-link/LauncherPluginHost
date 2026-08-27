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

/**
 * 用于监听插件连接与断开的接口。
 *
 * 插件回调的调用顺序为：
 *  1) {@link #onPluginAttached}
 *  2) {@link #onPluginLoaded}
 *  3) {@link #onPluginUnloaded}
 *  4) {@link #onPluginDetached}
 *
 * @param <T> 目标插件类型
 */
public interface PluginListener<T extends Plugin> {
    /**
     * 当插件已加载并可供使用时调用。
     * 如果允许多个插件，则可能会被多次调用。
     * 如果插件包发生变化需要重新加载，将来也可能会再次被调用。
     *
     * @deprecated 请迁移到 {@link #onPluginLoaded} 或 {@link #onPluginAttached}
     */
    @Deprecated
    default void onPluginConnected(T plugin, Context pluginContext) {
        // 可选实现
    }

    /**
     * 当插件首次挂载到宿主应用时调用。如果返回 true，则在首次挂载时也会自动调用
     * {@link #onPluginLoaded}。如果允许多个插件，则可能会被多次调用。
     * 如果插件包发生变化需要重新加载，将来也可能会再次被调用。
     * 每次调用 {@link #onPluginAttached} 都会提供一个新的或不同的 {@link PluginLifecycleManager}。
     *
     * @return 返回 true 将立即加载插件，并使用创建的对象调用 onPluginLoaded。
     *   返回 false 将跳过加载，但监听器可以使用所提供的 PluginLifecycleManager 随时加载。
     *   立即加载插件是默认行为。
     */
    default boolean onPluginAttached(PluginLifecycleManager<T> manager) {
        // 可选实现
        return true;
    }

    /**
     * 当插件已被卸载/更新、应从使用中移除时调用。
     *
     * @deprecated 请迁移到 {@link #onPluginDetached} 或 {@link #onPluginUnloaded}
     */
    @Deprecated
    default void onPluginDisconnected(T plugin) {
        // 可选实现。
    }

    /**
     * 当插件已从宿主应用上解除挂载时调用。实现者不应再尝试通过此
     * {@link PluginLifecycleManager} 重新加载它。如果包是被更新而非移除，
     * 那么当更新后的包可用时，将再次调用 {@link #onPluginAttached}。
     */
    default void onPluginDetached(PluginLifecycleManager<T> manager) {
        // 可选实现。
    }

    /**
     * 当插件被加载到宿主的进程中并可供使用时调用。如果客户端使用
     * {@link PluginLifecycleManager} 操纵插件的加载状态，这可能会发生多次。
     * 每次调用 {@link #onPluginLoaded} 都会在相应插件对象不再使用时，
     * 对应地调用一次 {@link #onPluginUnloaded}。
     */
    default void onPluginLoaded(
            T plugin,
            Context pluginContext,
            PluginLifecycleManager<T> manager
    ) {
        // 可选实现，默认调用已弃用的版本
        onPluginConnected(plugin, pluginContext);
    }

    /**
     * 当插件不应再被使用时调用。监听器应清理对相关插件的所有引用，
     * 以便其能够被垃圾回收。如果将来还需要该插件对象，
     * 可以调用 {@link PluginLifecycleManager#loadPlugin} 创建新的插件对象，
     * 并触发 {@link #onPluginLoaded}。
     */
    default void onPluginUnloaded(T plugin, PluginLifecycleManager<T> manager) {
        // 可选实现，默认调用已弃用的版本
        onPluginDisconnected(plugin);
    }
}
