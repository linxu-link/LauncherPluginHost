/*
 * Copyright (C) 2024 The Android Open Source Project
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

/** 用于接收由 ProtectedPluginProcessor 生成的代理类型所产生事件的监听器。 */
public interface ProtectedPluginListener {
    /**
     * 当一次方法调用在返回前产生 [Exception] 时被调用。提供此回调，
     * 以便宿主应用能够适当地终止插件或记录该错误。
     *
     * @return 返回 true 表示终止此对象内的所有方法；返回 false 表示该错误可恢复，
     *   被代理的插件应继续正常运行。
     */
    boolean onFail(String className, String methodName, Throwable failure);
}
