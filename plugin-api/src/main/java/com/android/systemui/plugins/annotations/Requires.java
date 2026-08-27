/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.systemui.plugins.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 用于标注某个插件所依赖的接口。
 *
 * 至少，所有插件都应对其正在实现的插件接口添加一个 @Requires 注解。
 * 同时，还需要为插件接口 @DependsOn 的每个类添加相应的 @Requires。
 */
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(value = Requirements.class)
public @interface Requires {
    Class<?> target();
    int version();
}
