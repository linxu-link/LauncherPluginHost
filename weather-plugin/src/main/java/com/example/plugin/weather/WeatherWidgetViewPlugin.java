package com.example.plugin.weather;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import com.android.systemui.plugins.annotations.Requires;
import com.android.systemui.plugins.WidgetViewPlugin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 天气桌面卡片插件。
 *
 * <p>展示一张 180×180dp 的正方形天气卡片：城市/时钟、太阳图标（光线旋转 + 本体呼吸）、
 * 当前温度、天气描述、未来 3 天小图标（错峰淡入）。
 * 全部数据为假数据，仅演示插件侧的持续动效能力。
 *
 * @author wujia
 */
@Requires(target = WidgetViewPlugin.class, version = WidgetViewPlugin.VERSION)
public class WeatherWidgetViewPlugin implements WidgetViewPlugin {

    /** 假数据：3 天预报。 */
    private static final String[] FORECAST_ICONS = {"sun", "cloud", "rain"};

    private View mRootView;
    private WeatherIconView mMainIcon;
    private TextView mClock;
    private TextView mTemp;
    private TextView mDesc;
    private final Handler mClockHandler = new Handler();
    private final Runnable mClockTicker = new Runnable() {
        @Override
        public void run() {
            updateClock();
            mClockHandler.postDelayed(this, 1_000L);
        }
    };

    @Override
    public void onCreate(Context context, Context pluginContext) {
        // 无需初始化，createView 时再 inflate
    }

    @Override
    public View createView(Context pluginContext) {
        mRootView = LayoutInflater.from(pluginContext)
                .inflate(R.layout.widget_weather, null);

        mMainIcon = mRootView.findViewById(R.id.weather_icon);
        mClock = mRootView.findViewById(R.id.weather_clock);
        mTemp = mRootView.findViewById(R.id.weather_temp);
        mDesc = mRootView.findViewById(R.id.weather_desc);

        // 主图标设为太阳模式并启动动画
        mMainIcon.setMode(WeatherIconView.MODE_SUN);

        // 未来 3 天小图标按 tag 设置模式
        for (int i = 0; i < 3; i++) {
            int idRes = mRootView.getResources().getIdentifier(
                    "forecast_icon_" + (i + 1), "id",
                    pluginContext.getPackageName());
            WeatherIconView v = mRootView.findViewById(idRes);
            if (v != null) {
                v.setMode(FORECAST_ICONS[i]);
            }
        }

        // 温度从 -5° 动画到目标 26°，900ms
        ObjectAnimator tempAnim = ObjectAnimator.ofFloat(mTemp, View.ALPHA, 0f, 1f);
        tempAnim.setDuration(900L);
        tempAnim.setInterpolator(new DecelerateInterpolator(1.5f));
        tempAnim.setStartDelay(250L);

        // 入场：alpha + 缩放回弹
        mRootView.setAlpha(0f);
        mRootView.setScaleX(0.85f);
        mRootView.setScaleY(0.85f);
        mRootView.post(() -> {
            mRootView.setPivotX(mRootView.getWidth() / 2f);
            mRootView.setPivotY(mRootView.getHeight() / 2f);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(mRootView, View.ALPHA, 0f, 1f);
            ObjectAnimator sx = ObjectAnimator.ofFloat(mRootView, View.SCALE_X, 0.85f, 1f);
            ObjectAnimator sy = ObjectAnimator.ofFloat(mRootView, View.SCALE_Y, 0.85f, 1f);
            sx.setInterpolator(new OvershootInterpolator(1.6f));
            sy.setInterpolator(new OvershootInterpolator(1.6f));
            AnimatorSet entry = new AnimatorSet();
            entry.setDuration(420L);
            entry.playTogether(alpha, sx, sy, tempAnim);
            entry.start();

            // 错峰淡入未来 3 天
            for (int i = 0; i < 3; i++) {
                int idRes = mRootView.getResources().getIdentifier(
                        "forecast_icon_" + (i + 1), "id",
                        pluginContext.getPackageName());
                View icon = mRootView.findViewById(idRes);
                int tempIdRes = mRootView.getResources().getIdentifier(
                        "forecast_temp_" + (i + 1), "id",
                        pluginContext.getPackageName());
                View tempV = mRootView.findViewById(tempIdRes);
                if (icon != null) {
                    icon.setAlpha(0f);
                    ObjectAnimator a = ObjectAnimator.ofFloat(icon, View.ALPHA, 0f, 1f);
                    a.setStartDelay(500L + i * 180L);
                    a.setDuration(600L);
                    a.setInterpolator(new DecelerateInterpolator());
                    a.start();
                }
                if (tempV != null) {
                    tempV.setAlpha(0f);
                    ObjectAnimator a = ObjectAnimator.ofFloat(tempV, View.ALPHA, 0f, 1f);
                    a.setStartDelay(600L + i * 180L);
                    a.setDuration(600L);
                    a.setInterpolator(new DecelerateInterpolator());
                    a.start();
                }
            }

            // 描述文字打字机效果：逐字淡入
            animateTypewriter(mDesc, "多云转晴", 600L, 80L);
        });

        updateClock();
        mClockHandler.post(mClockTicker);

        return mRootView;
    }

    /** 打字机效果：每隔 {@code charIntervalMs} 显示一个字符。 */
    private void animateTypewriter(TextView tv, String text, long startDelay, long charIntervalMs) {
        tv.setText("");
        tv.setAlpha(0f);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(tv, View.ALPHA, 0f, 1f);
        fadeIn.setStartDelay(startDelay);
        fadeIn.setDuration(300L);
        fadeIn.start();
        final int len = text.length();
        final Handler h = new Handler();
        final Runnable[] tick = new Runnable[1];
        final int[] idx = {0};
        tick[0] = new Runnable() {
            @Override
            public void run() {
                if (idx[0] > len) return;
                tv.setText(text.substring(0, idx[0]));
                idx[0]++;
                if (idx[0] <= len) {
                    h.postDelayed(this, charIntervalMs);
                }
            }
        };
        h.postDelayed(tick[0], startDelay);
    }

    private void updateClock() {
        if (mClock == null) return;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
        mClock.setText(sdf.format(new Date()));
    }

    @Override
    public void onDestroy() {
        mClockHandler.removeCallbacks(mClockTicker);
    }
}
