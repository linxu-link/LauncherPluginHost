/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.shared.plugins;

import android.annotation.IntDef;
import android.content.ComponentName;

/**
 * 启用与禁用插件。
 */
public interface PluginEnabler {

    int ENABLED = 0;
    int DISABLED_MANUALLY = 1;
    int DISABLED_INVALID_VERSION = 2;
    int DISABLED_FROM_EXPLICIT_CRASH = 3;
    int DISABLED_FROM_SYSTEM_CRASH = 4;
    int DISABLED_UNKNOWN = 100;

    @IntDef({ENABLED, DISABLED_MANUALLY, DISABLED_INVALID_VERSION, DISABLED_FROM_EXPLICIT_CRASH,
            DISABLED_FROM_SYSTEM_CRASH, DISABLED_UNKNOWN})
    @interface DisableReason {
    }

    /** 通过 PackageManager 启用插件。 */
    void setEnabled(ComponentName component);

    /** 通过 PackageManager 禁用插件，并记录禁用原因。 */
    void setDisabled(ComponentName component, @DisableReason int reason);

    /** 返回插件在 PackageManager 中是否处于启用状态。 */
    boolean isEnabled(ComponentName component);

    /**
     * 返回插件被禁用的原因（若已被禁用）。
     *
     * <p>插件启用时应返回 {@link #ENABLED}；
     * 插件被关闭但原因未知时应返回 {@link #DISABLED_MANUALLY}。
     */
    @DisableReason
    int getDisableReason(ComponentName componentName);
}
