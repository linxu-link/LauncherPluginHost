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
package com.android.systemui.plugins.shared;

import android.view.MotionEvent;

import java.io.PrintWriter;

/**
 * 用于控制 Launcher 上的覆盖层（overlay）的接口
 */
public interface LauncherOverlayManager {

    default void onDeviceProvideChanged() { }

    default void onAttachedToWindow() { }
    default void onDetachedFromWindow() { }

    default void dump(String prefix, PrintWriter w) { }

    default void openOverlay() { }

    default void hideOverlay(boolean animate) {
        hideOverlay(animate ? 200 : 0);
    }

    default void hideOverlay(int duration) { }

    default void onActivityStarted() { }

    default void onActivityResumed() { }

    default void onActivityPaused() { }

    default void onActivityStopped() { }

    default void onActivityDestroyed() { }

    default void onDisallowSwipeToMinusOnePage() {}

    /**
     * @deprecated 请直接使用 LauncherOverlayTouchProxy
     */
    @Deprecated
    interface LauncherOverlay extends LauncherOverlayTouchProxy {

        /**
         * 导致过度滚动（overscroll）的触摸交互已经开始
         */
        void onScrollInteractionBegin();

        /**
         * 与过度滚动（overscroll）相关的触摸交互已结束
         */
        void onScrollInteractionEnd();

        /**
         * 当用户滚动超过最左侧屏幕（或在 RTL 布局下为最右侧屏幕）时的滚动进度，取值介于 0 到 100 之间。
         */
        void onScrollChange(float progress, boolean rtl);

        /**
         * 当 Launcher 准备好使用覆盖层时调用
         * @param callbacks 由 Launcher 提供的与覆盖层相关的一组回调
         */
        void setOverlayCallbacks(LauncherOverlayCallbacks callbacks);

        @Override
        default void onFlingVelocity(float velocity) { }

        @Override
        default void onOverlayMotionEvent(MotionEvent ev, float scrollProgress) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    onScrollInteractionBegin();
                    break;
                case MotionEvent.ACTION_MOVE:
                    onScrollChange(scrollProgress, false);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    onScrollInteractionEnd();
                    break;
                default:
                    break;
            }
        }
    }

    interface LauncherOverlayTouchProxy {

        /**
         * 在结束滚动交互之前调用，用于指示 fling（快速滑动）速度
         */
        void onFlingVelocity(float velocity);

        /**
         * 用于向覆盖层分发各种触摸事件
         */
        void onOverlayMotionEvent(MotionEvent ev, float scrollProgress);

        /**
         * 当 Launcher 准备好使用覆盖层时调用
         * @param callbacks 由 Launcher 提供的与覆盖层相关的一组回调
         */
        default void setOverlayCallbacks(LauncherOverlayCallbacks callbacks) { }
    }

    interface LauncherOverlayCallbacks {

        void onOverlayScrollChanged(float progress);
    }
}
