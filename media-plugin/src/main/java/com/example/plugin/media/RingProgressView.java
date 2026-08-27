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

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.Locale;

/**
 * 插件内置的圆环进度自定义控件。
 *
 * <p>作为插件私有类，只能在插件自己的 Context（宿主通过
 * {@link com.android.systemui.plugins.WidgetViewPlugin#createView(android.content.Context)} 提供）
 * 下被实例化。
 *
 * @author wujia
 */
public class RingProgressView extends View {

    private final Paint mTrackPaint;
    private final Paint mProgressPaint;
    private final Paint mTextPaint;
    private final RectF mArcRect = new RectF();

    private float mProgress = 0f;
    private CharSequence mCenterText = "";
    private ValueAnimator mLoopAnimator;

    public RingProgressView(Context context) {
        this(context, null);
    }

    public RingProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RingProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTrackPaint.setStyle(Paint.Style.STROKE);
        mTrackPaint.setStrokeWidth(dp(4));
        mTrackPaint.setStrokeCap(Paint.Cap.ROUND);
        mTrackPaint.setColor(Color.rgb(42, 52, 68));

        mProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mProgressPaint.setStyle(Paint.Style.STROKE);
        mProgressPaint.setStrokeWidth(dp(4));
        mProgressPaint.setStrokeCap(Paint.Cap.ROUND);
        mProgressPaint.setColor(Color.rgb(79, 195, 247));

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(dp(13));
        mTextPaint.setFakeBoldText(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setProgress(float progress) {
        mProgress = Math.max(0f, Math.min(100f, progress));
        invalidate();
    }

    public void setCenterText(CharSequence text) {
        mCenterText = text;
        invalidate();
    }

    /**
     * 让圆环从 0 匀速到 100 无限循环，按 {@code totalSeconds} 将进度映射为播放时间，
     * 并在中心实时显示 m:ss 文本。用于模拟"正在播放"的持续动效。
     */
    public void startProgressLoop(long durationMs, int totalSeconds) {
        stopProgressLoop();
        mLoopAnimator = ValueAnimator.ofFloat(0f, 100f);
        mLoopAnimator.setDuration(durationMs);
        mLoopAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mLoopAnimator.setRepeatMode(ValueAnimator.RESTART);
        mLoopAnimator.setInterpolator(new LinearInterpolator());
        mLoopAnimator.addUpdateListener(a -> {
            mProgress = (Float) a.getAnimatedValue();
            int curSec = (int) (totalSeconds * mProgress / 100f);
            mCenterText = formatTime(curSec);
            invalidate();
        });
        mLoopAnimator.start();
    }

    /** 停止持续循环动画。 */
    public void stopProgressLoop() {
        if (mLoopAnimator != null) {
            mLoopAnimator.cancel();
            mLoopAnimator = null;
        }
    }

    private static String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format(Locale.US, "%d:%02d", m, s);
    }

    /** 取消循环动画。 */
    public void cancelAnimation() {
        stopProgressLoop();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float stroke = dp(4);
        float inset = stroke / 2f + dp(2);
        float size = Math.min(getWidth(), getHeight());
        mArcRect.set(inset, inset, size - inset, size - inset);

        canvas.drawArc(mArcRect, 0f, 360f, false, mTrackPaint);

        float sweep = 360f * (mProgress / 100f);
        canvas.drawArc(mArcRect, -90f, sweep, false, mProgressPaint);

        float cy = getHeight() / 2f;
        float baseline = cy - (mTextPaint.ascent() + mTextPaint.descent()) / 2f;
        canvas.drawText(mCenterText.toString(), getWidth() / 2f, baseline, mTextPaint);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
