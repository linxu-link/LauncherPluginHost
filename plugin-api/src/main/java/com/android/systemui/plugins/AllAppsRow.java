/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.view.View;
import android.view.ViewGroup;

import com.android.systemui.plugins.annotations.ProvidesInterface;

/**
 * 实现此插件接口，可在所有应用抽屉（all apps drawer）顶部添加一行视图。
 */
@ProvidesInterface(action = AllAppsRow.ACTION, version = AllAppsRow.VERSION)
public interface AllAppsRow extends Plugin {
    String ACTION = "com.android.systemui.action.PLUGIN_ALL_APPS_ACTIONS";
    int VERSION = 1;

    /**
     * 设置该行并返回父视图。
     * @param parent Launcher 将把此行添加到的 ViewGroup。
     */
    View setup(ViewGroup parent);

    /**
     * @return 需要在所有应用中为你的视图预留的高度。
     */
    int getExpectedHeight();

    /**
     * 每当 {@link #getExpectedHeight()} 发生变化时，通知 Launcher 更新。
     */
    void setOnHeightUpdatedListener(OnHeightUpdatedListener onHeightUpdatedListener);

    interface OnHeightUpdatedListener {
        void onHeightUpdated();
    }
}
