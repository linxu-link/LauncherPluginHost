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
 * 用于表示插件库中的某个接口需要另一个接口才能正常工作。
 * 添加此注解后，将强制所有 @Requires 该被注解接口的插件，
 * 同时也必须 @Requires 所指定的类。
 */
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(value = Dependencies.class)
public @interface DependsOn {
    Class<?> target();

}
