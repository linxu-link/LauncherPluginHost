package com.example.plugin.weather;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * 天气图标自定义控件。
 *
 * <p>支持三种图标模式：sun（太阳，带旋转光线）、cloud（云）、rain（雨）。
 * 通过 {@code android:tag} 在 XML 中指定模式，也可用 {@link #setMode(String)} 切换。
 * 太阳模式会自带光线旋转动画。
 *
 * @author wujia
 */
public class WeatherIconView extends View {

    public static final String MODE_SUN = "sun";
    public static final String MODE_CLOUD = "cloud";
    public static final String MODE_RAIN = "rain";

    private final Paint mFillPaint;
    private final Paint mStrokePaint;
    private final Path mCloudPath = new Path();

    private String mMode = MODE_SUN;
    private float mRotation;          // 太阳光线当前旋转角度
    private float mSunPulse = 1f;     // 太阳本体呼吸系数 0.92-1.0
    private ValueAnimator mSpinAnimator;
    private ValueAnimator mPulseAnimator;
    private boolean mAnimating;

    public WeatherIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFillPaint.setStyle(Paint.Style.FILL);
        mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setStrokeCap(Paint.Cap.ROUND);
        // XML tag 决定模式
        Object tag = getTag();
        if (tag != null) {
            setMode(tag.toString());
        }
    }

    /** 设置天气图标模式：sun / cloud / rain。 */
    public void setMode(String mode) {
        mMode = mode;
        // 主图标（无 tag 时默认 sun）开启动画
        boolean shouldAnimate = MODE_SUN.equals(mode) && getId() == R.id.weather_icon;
        if (shouldAnimate) {
            ensureAnimators();
        } else {
            stopAnimators();
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(w, h) * 0.28f;

        switch (mMode) {
            case MODE_SUN:
                drawSun(canvas, cx, cy, radius);
                break;
            case MODE_CLOUD:
                drawCloud(canvas, w, h);
                break;
            case MODE_RAIN:
                drawCloud(canvas, w, (int) (h * 0.7f));
                drawRain(canvas, w, h);
                break;
        }
    }

    /** 太阳：旋转光线 + 脉动本体。 */
    private void drawSun(Canvas canvas, float cx, float cy, float radius) {
        float sunR = radius * mSunPulse;

        // 外圈光线
        mStrokePaint.setColor(getResources().getColor(R.color.weather_sun));
        mStrokePaint.setStrokeWidth(radius * 0.1f);
        canvas.save();
        canvas.rotate(mRotation, cx, cy);
        int rayCount = 8;
        float rayLen = radius * 0.55f;
        for (int i = 0; i < rayCount; i++) {
            float angle = (float) (i * 2 * Math.PI / rayCount);
            float x1 = cx + (float) Math.cos(angle) * (sunR * 1.3f);
            float y1 = cy + (float) Math.sin(angle) * (sunR * 1.3f);
            float x2 = cx + (float) Math.cos(angle) * (sunR * 1.3f + rayLen);
            float y2 = cy + (float) Math.sin(angle) * (sunR * 1.3f + rayLen);
            canvas.drawLine(x1, y1, x2, y2, mStrokePaint);
        }
        canvas.restore();

        // 太阳本体：径向渐变感（用两层圆模拟）
        mFillPaint.setColor(getResources().getColor(R.color.weather_sun));
        canvas.drawCircle(cx, cy, sunR, mFillPaint);
        mFillPaint.setColor(0xFFFFEB3B);
        canvas.drawCircle(cx, cy, sunR * 0.7f, mFillPaint);
    }

    /** 云：三个圆 + 底部矩形拼出轮廓。 */
    private void drawCloud(Canvas canvas, int w, int h) {
        mFillPaint.setColor(getResources().getColor(R.color.weather_cloud));
        float cx = w / 2f;
        float cy = h / 2f;
        float r = Math.min(w, h) * 0.24f;
        canvas.drawCircle(cx - r * 0.7f, cy + r * 0.2f, r, mFillPaint);
        canvas.drawCircle(cx + r * 0.7f, cy + r * 0.2f, r, mFillPaint);
        canvas.drawCircle(cx, cy - r * 0.3f, r * 1.2f, mFillPaint);
        mCloudPath.reset();
        mCloudPath.addRect(cx - r * 1.4f, cy + r * 0.2f - r, cx + r * 1.4f, cy + r * 0.2f + r,
                Path.Direction.CW);
        canvas.drawPath(mCloudPath, mFillPaint);
    }

    /** 雨：云下方斜线。 */
    private void drawRain(Canvas canvas, int w, int h) {
        mStrokePaint.setColor(getResources().getColor(R.color.weather_rain));
        mStrokePaint.setStrokeWidth(w * 0.05f);
        float cx = w / 2f;
        float topY = h * 0.55f;
        float[] xs = {cx - w * 0.18f, cx, cx + w * 0.18f};
        for (float x : xs) {
            canvas.drawLine(x, topY, x - w * 0.08f, topY + h * 0.22f, mStrokePaint);
        }
    }

    private void ensureAnimators() {
        if (mAnimating) return;
        mAnimating = true;
        // 光线匀速旋转，6s/圈
        mSpinAnimator = ValueAnimator.ofFloat(0f, 360f);
        mSpinAnimator.setDuration(6_000L);
        mSpinAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mSpinAnimator.setRepeatMode(ValueAnimator.RESTART);
        mSpinAnimator.setInterpolator(new LinearInterpolator());
        mSpinAnimator.addUpdateListener(a -> {
            mRotation = (Float) a.getAnimatedValue();
            invalidate();
        });
        mSpinAnimator.start();
        // 本体呼吸 0.92-1.0，2.4s 来回
        mPulseAnimator = ValueAnimator.ofFloat(0.92f, 1f);
        mPulseAnimator.setDuration(2_400L);
        mPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        mPulseAnimator.addUpdateListener(a -> {
            mSunPulse = (Float) a.getAnimatedValue();
            invalidate();
        });
        mPulseAnimator.start();
    }

    private void stopAnimators() {
        if (!mAnimating) return;
        mAnimating = false;
        if (mSpinAnimator != null) mSpinAnimator.cancel();
        if (mPulseAnimator != null) mPulseAnimator.cancel();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimators();
    }
}
