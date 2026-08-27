/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;

import com.android.systemui.plugins.annotations.ProvidesInterface;

/**
 * 实现此插件接口以添加自定义小部件。
 */
@ProvidesInterface(action = CustomWidgetPlugin.ACTION, version = CustomWidgetPlugin.VERSION)
public interface CustomWidgetPlugin extends Plugin {

    String ACTION = "com.android.systemui.action.PLUGIN_CUSTOM_WIDGET";
    int VERSION = 1;

    /**
     * 通知插件小部件的容器已渲染完成，自定义小部件可以附着到该容器上。
     */
    void onViewCreated(AppWidgetHostView parent);

    /**
     * 获取自定义小部件的 UUID。
     *
     * @deprecated 已不再使用
     */
    @Deprecated
    default String getId() {
        return "";
    }

    /**
     * 用于修改小部件的信息。
     */
    default void updateWidgetInfo(AppWidgetProviderInfo info, Context context) { }
}
