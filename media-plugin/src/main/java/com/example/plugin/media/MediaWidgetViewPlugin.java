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
package com.example.plugin.media;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import com.android.systemui.plugins.WidgetViewPlugin;
import com.android.systemui.plugins.annotations.Requires;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 多媒体桌面卡片插件。
 *
 * <p>展示一张紧凑的"正在播放"卡片：旋转唱片、循环进度环、滚动歌词、呼吸播放按钮与实时时钟。
 * 未接入真实媒体后端，所有效果均由动画驱动，用于演示插件侧持续动效能力。
 *
 * @author wujia
 */
@Requires(target = WidgetViewPlugin.class, version = WidgetViewPlugin.VERSION)
public class MediaWidgetViewPlugin implements WidgetViewPlugin {
    private static final String TAG = "MediaWidgetViewPlugin";

    /** 假歌曲总时长（秒），驱动进度环中心时间文本。 */
    private static final int SONG_SECONDS = 240;
    /** 进度环一圈时长（毫秒），模拟一首完整歌曲的播放节奏。 */
    private static final long RING_LOOP_MS = 24_000L;
    /** 唱片旋转一圈周期（毫秒）。 */
    private static final long DISC_SPIN_MS = 3_000L;
    /** 每句歌词停留时长（毫秒）。 */
    private static final long LYRIC_INTERVAL_MS = 3_500L;
    /** 歌词淡入淡出时长（毫秒）。 */
    private static final long LYRIC_FADE_MS = 450L;

    private static final String[] LYRICS = new String[]{
            "夜空中最亮的星",
            "请指引我靠近你",
            "夜空中最亮的星",
            "是否记得曾和我同行的身影",
            "给我再去相信的勇气",
            "越过谎言去拥抱你",
            "每当找不到存在的意义",
            "每当我迷失在黑夜里",
    };

    private View mRootView;
    private TextView mClockText;
    private RingProgressView mRing;
    private View mDisc;
    private TextView mLyric;
    private View mPlayBtn;

    private ObjectAnimator mDiscSpin;
    private ObjectAnimator mPlayPulse;
    private ValueAnimator mLyricFader;

    private int mLyricIndex = 0;

    private final Runnable mClockTicker = new Runnable() {
        @Override
        public void run() {
            updateClock();
            if (mRootView != null) mRootView.postDelayed(this, 1_000L);
        }
    };

    private final Runnable mLyricTicker = new Runnable() {
        @Override
        public void run() {
            cycleLyric();
            if (mRootView != null) mRootView.postDelayed(this, LYRIC_INTERVAL_MS);
        }
    };

    @Override
    public void onCreate(Context sysuiContext, Context pluginContext) {
        Log.d(TAG, "onCreate pluginContext=" + pluginContext);
    }

    @Override
    public View createView(Context pluginContext) {
        if (mRootView != null) return mRootView;
        Log.d(TAG, "createView");

        LayoutInflater inflater = LayoutInflater.from(pluginContext);
        mRootView = inflater.inflate(R.layout.widget_view, null);

        mClockText = mRootView.findViewById(R.id.widget_clock);
        mRing = mRootView.findViewById(R.id.widget_ring_progress);
        mDisc = mRootView.findViewById(R.id.widget_disc);
        mLyric = mRootView.findViewById(R.id.widget_lyric);
        mPlayBtn = mRootView.findViewById(R.id.widget_btn_play);

        mRing.setProgress(0f);
        mRing.setCenterText("0:00");
        mLyric.setText(LYRICS[0]);
        mLyric.setAlpha(0f);

        updateClock();
        mRootView.post(mClockTicker);
        mRootView.post(mLyricTicker);
        // 入场后 300ms 淡入第一句歌词
        mRootView.postDelayed(this::fadeInLyric, 300L);
        // 布局完成后启动持续动效
        mRootView.post(this::startContinuousAnimations);
        return mRootView;
    }

    private void startContinuousAnimations() {
        if (mRootView == null) return;

        // 1) 入场动画：alpha + 缩放带回弹（一次性）
        mRootView.setAlpha(0f);
        mRootView.setScaleX(0.85f);
        mRootView.setScaleY(0.85f);
        mRootView.setPivotX(mRootView.getWidth() / 2f);
        mRootView.setPivotY(mRootView.getHeight() / 2f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(mRootView, View.ALPHA, 0f, 1f);
        ObjectAnimator sx = ObjectAnimator.ofFloat(mRootView, View.SCALE_X, 0.85f, 1f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(mRootView, View.SCALE_Y, 0.85f, 1f);
        sx.setInterpolator(new OvershootInterpolator(1.6f));
        sy.setInterpolator(new OvershootInterpolator(1.6f));
        AnimatorSet entry = new AnimatorSet();
        entry.setDuration(420L);
        entry.playTogether(alpha, sx, sy);
        entry.start();

        // 2) 唱片匀速旋转，无限循环
        if (mDisc != null) {
            mDiscSpin = ObjectAnimator.ofFloat(mDisc, View.ROTATION, 0f, 360f);
            mDiscSpin.setDuration(DISC_SPIN_MS);
            mDiscSpin.setRepeatCount(ValueAnimator.INFINITE);
            mDiscSpin.setRepeatMode(ValueAnimator.RESTART);
            mDiscSpin.setInterpolator(new LinearInterpolator());
            mDiscSpin.start();
        }

        // 3) 进度环 0→100 无限循环，同步更新中心播放时间
        if (mRing != null) {
            mRing.startProgressLoop(RING_LOOP_MS, SONG_SECONDS);
        }

        // 4) 播放按钮呼吸脉冲（alpha 缓慢起伏）
        if (mPlayBtn != null) {
            mPlayPulse = ObjectAnimator.ofFloat(mPlayBtn, View.ALPHA, 1f, 0.55f);
            mPlayPulse.setDuration(900L);
            mPlayPulse.setRepeatCount(ValueAnimator.INFINITE);
            mPlayPulse.setRepeatMode(ValueAnimator.REVERSE);
            mPlayPulse.setInterpolator(new AccelerateDecelerateInterpolator());
            mPlayPulse.start();
        }
    }

    private void cycleLyric() {
        if (mLyric == null || mRootView == null) return;
        // 先淡出 → 切换文字 → 再淡入
        mLyricFader = ObjectAnimator.ofFloat(mLyric, View.ALPHA,
                mLyric.getAlpha(), 0f);
        mLyricFader.setDuration(LYRIC_FADE_MS);
        mLyricFader.setInterpolator(new AccelerateDecelerateInterpolator());
        mLyricFader.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (mLyric == null) return;
                mLyricIndex = (mLyricIndex + 1) % LYRICS.length;
                mLyric.setText(LYRICS[mLyricIndex]);
                ObjectAnimator in = ObjectAnimator.ofFloat(mLyric, View.ALPHA, 0f, 1f);
                in.setDuration(LYRIC_FADE_MS);
                in.setInterpolator(new AccelerateDecelerateInterpolator());
                in.start();
            }
        });
        mLyricFader.start();
    }

    private void fadeInLyric() {
        if (mLyric == null) return;
        ObjectAnimator in = ObjectAnimator.ofFloat(mLyric, View.ALPHA, 0f, 1f);
        in.setDuration(LYRIC_FADE_MS);
        in.setInterpolator(new AccelerateDecelerateInterpolator());
        in.start();
    }

    private void updateClock() {
        if (mClockText != null) {
            mClockText.setText(new SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(new Date()));
        }
    }

    @Override
    public void onViewDestroyed() {
        Log.d(TAG, "onViewDestroyed");
        if (mRootView != null) {
            mRootView.removeCallbacks(mClockTicker);
            mRootView.removeCallbacks(mLyricTicker);
        }
        if (mDiscSpin != null) { mDiscSpin.cancel(); mDiscSpin = null; }
        if (mPlayPulse != null) { mPlayPulse.cancel(); mPlayPulse = null; }
        if (mLyricFader != null) { mLyricFader.cancel(); mLyricFader = null; }
        if (mRing != null) { mRing.cancelAnimation(); }
        mRootView = null;
        mClockText = null;
        mRing = null;
        mDisc = null;
        mLyric = null;
        mPlayBtn = null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        onViewDestroyed();
    }
}
