package com.letv.commonjar.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.SystemProperties;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

public class JScreenTool {

    public static DisplayMetrics getDisplayMetrics(Activity ctx) {
        DisplayMetrics metric = new DisplayMetrics();
        ctx.getWindowManager().getDefaultDisplay().getMetrics(metric);
        return metric;
    }

    public static int[] getSize(Activity ctx) {
        int[] size = {0, 0};
        DisplayMetrics metric = new DisplayMetrics();
        ctx.getWindowManager().getDefaultDisplay().getMetrics(metric);
        size[0] = metric.widthPixels;
        size[1] = metric.heightPixels;
        return size;
    }

    public static float getDensity(Activity ctx) {
        DisplayMetrics metric = new DisplayMetrics();
        ctx.getWindowManager().getDefaultDisplay().getMetrics(metric);
        return metric.density;
    }

    public static int getf(Activity ctx) {
        DisplayMetrics metric = new DisplayMetrics();
        ctx.getWindowManager().getDefaultDisplay().getMetrics(metric);
        return metric.densityDpi;
    }

    private static float getDegreesForRotation(int value) {
        switch (value) {
            case Surface.ROTATION_90:
                return 360f - 90f;
            case Surface.ROTATION_180:
                return 360f - 180f;
            case Surface.ROTATION_270:
                return 360f - 270f;
        }
        return 0f;
    }

    /**
     * caller must add <android:sharedUserId="android.uid.system"> in AndroidManifest.xml
     * 
     * @param ctx
     * @return screenshot bitmap
     */
    public static Bitmap takeScreenshot(Context ctx) {
        WindowManager mWindowManager = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        Display mDisplay = mWindowManager.getDefaultDisplay();
        DisplayMetrics mDisplayMetrics = new DisplayMetrics();
        mDisplay.getRealMetrics(mDisplayMetrics);
        Matrix mDisplayMatrix = new Matrix();
        float[] dims = {mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels};
        int value = mDisplay.getRotation();
        String hwRotation = SystemProperties.get("ro.sf.hwrotation", "0");
        if (hwRotation.equals("270") || hwRotation.equals("90")) {
            value = (value + 3) % 4;
        }
        float degrees = getDegreesForRotation(value);
        boolean requiresRotation = (degrees > 0);
        if (requiresRotation) {
            // Get the dimensions of the device in its native orientation
            mDisplayMatrix.reset();
            mDisplayMatrix.preRotate(-degrees);
            mDisplayMatrix.mapPoints(dims);
            dims[0] = Math.abs(dims[0]);
            dims[1] = Math.abs(dims[1]);
        }

        Bitmap mScreenBitmap = null;// Surface.screenshot((int) dims[0], (int) dims[1]);

        if (requiresRotation && mScreenBitmap != null) {
            // Rotate the screenshot to the current orientation
            Bitmap ss = Bitmap.createBitmap(mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(ss);
            c.translate(ss.getWidth() / 2, ss.getHeight() / 2);
            c.rotate(degrees);
            c.translate(-dims[0] / 2, -dims[1] / 2);
            c.drawBitmap(mScreenBitmap, 0, 0, null);
            c.setBitmap(null);
            mScreenBitmap = ss;
        }

        if (mScreenBitmap == null) {
            return null;
        }

        // Optimizations
        mScreenBitmap.setHasAlpha(false);
        mScreenBitmap.prepareToDraw();

        return mScreenBitmap;
    }
}
