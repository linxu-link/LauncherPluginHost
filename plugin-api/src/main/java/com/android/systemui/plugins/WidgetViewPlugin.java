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
package com.android.systemui.plugins;

import android.content.Context;
import android.view.View;

import com.android.systemui.plugins.annotations.ProvidesInterface;

/**
 * 提供一种类似自定义小部件的视图的插件，宿主会将其附加到自身 UI 上。
 *
 * <p>这演示了“插件提供复杂自定义视图”的使用场景：插件使用自己的
 * {@code pluginContext} 构建并返回一个 {@link View}（这样它可以加载自己的布局和资源）。
 * 宿主只需将返回的视图添加到某个容器中。
 *
 * <p>返回的视图必须在 {@link #createView(Context)} 内部惰性创建，并且必须使用传入的
 * {@code pluginContext} 创建（而不是使用宿主上下文），否则无法解析插件 APK 内定义的自定义视图类。
 */
@ProvidesInterface(action = WidgetViewPlugin.ACTION, version = WidgetViewPlugin.VERSION)
public interface WidgetViewPlugin extends Plugin {

    String ACTION = "com.android.systemui.action.PLUGIN_WIDGET_VIEW";
    int VERSION = 1;

    /**
     * 创建宿主将附加到其 UI 上的自定义小部件视图。
     *
     * @param pluginContext 插件的上下文；使用它来加载布局 / 资源
     * @return 完全构建好的视图；如果插件无法构建，则返回 {@code null}
     */
    View createView(Context pluginContext);

    /**
     * 当之前返回的视图已被移除、插件应释放其持有的所有资源（动画、监听器等）时，
     * 由宿主调用此方法。
     */
    default void onViewDestroyed() {
    }
}
