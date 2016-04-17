package com.bbbbiu.biu.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;

/**
 * http://stackoverflow.com/questions/4605527/converting-pixels-to-dp
 * The above method results accurate method compared to below methods
 * http://stackoverflow.com/questions/8309354/formula-px-to-dp-dp-to-px-android
 * <p/>
 * Created by YieldNull at 4/7/16
 */
public class SizeUtil {
    private static final String TAG = SizeUtil.class.getSimpleName();

    public static float convertPixelsToDp(float px) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float dp = px / (metrics.densityDpi / 160f);
        return Math.round(dp);
    }

    public static float convertDpToPixel(float dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return Math.round(px);
    }

    public static float getScreenWidth(Activity context) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        return displaymetrics.widthPixels;
    }
}
