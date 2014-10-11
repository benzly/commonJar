/**
 * =====================================================================
 *
 * @file JImage.java
 * @Module Name com.joysee.common.utils
 * @author xubin
 * @OS version 1.0
 * @Product type: JoySee
 * @date 2013-12-17
 * @brief This file is the http **** implementation.
 * @This file is responsible by ANDROID TEAM.
 * @Comments: ===================================================================== Revision
 *            History:
 *
 *            Modification Tracking
 *
 *            Author Date OS version Reason ---------- ------------ ------------- ----------- xubin
 *            2013-12-17 1.0 Check for NULL, 0 h/w
 *            =====================================================================
 **/

package com.letv.commonjar.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

public class JImage {

    /**
     * @param originalImage 原图
     * @param invertedImageH 倒影高度
     * @return 倒影图片，消耗较大
     */
    public static Bitmap getReflect(Bitmap originalImage, int invertedImageH, boolean isMerge) {
        final int reflectionGap = 0;
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        Matrix matrix = new Matrix();
        matrix.preScale(1, -1);
        Bitmap reflectionImage = Bitmap.createBitmap(originalImage, 0, (height - invertedImageH), width, invertedImageH, matrix, false);

        if (!isMerge) {
            return reflectionImage;
        }

        Bitmap bitmapWithReflection = Bitmap.createBitmap(width, (height + invertedImageH), Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapWithReflection);
        canvas.drawBitmap(originalImage, 0, 0, null);
        Paint defaultPaint = new Paint();
        canvas.drawRect(0, height, width, height + reflectionGap, defaultPaint);
        canvas.drawBitmap(reflectionImage, 0, height + reflectionGap, null);
        Paint paint = new Paint();
        LinearGradient shader = new LinearGradient(0, originalImage.getHeight(), 0, bitmapWithReflection.getHeight() + //
                reflectionGap, 0x70ffffff, 0x00ffffff, TileMode.MIRROR);
        paint.setShader(shader);
        paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
        canvas.drawRect(0, height, width, bitmapWithReflection.getHeight() + reflectionGap, paint);

        return bitmapWithReflection;
    }

    /**
     * @param bitmap
     * @param roundPx
     * @return 圆角图片，消耗较大
     */
    public static Bitmap getRound(Bitmap bitmap, float roundPx) {
        if (bitmap == null) {
            return null;
        }
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    /**
     * @param originalImage 原图
     * @param invertedImageH 倒影高度
     * @param roundPx 圆角角度
     * @return 倒影加圆角图片，消耗较大
     */
    public static Bitmap getRoundReflect(Bitmap originalImage, int invertedImageH, float roundPx) {
        return getRound(getReflect(originalImage, invertedImageH, true), roundPx);
    }

    /**
     * @param imageView
     * @param saturation 0.0f - 1.0f
     */
    public static void setGray(ImageView imageView, int saturation) {
        if (imageView != null) {
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(saturation);
            imageView.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        }
    }

    /**
     * @param drawable
     * @param saturation 0.0f - 1.0f
     */
    public static void setGray(Drawable drawable, int saturation) {
        if (drawable != null) {
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(saturation);
            drawable.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        }
    }

    /**
     * @param bitmap
     * @return 黑白图片，消耗较大
     */
    public static Bitmap getBlack(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        int alpha = 0xFF << 24;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int grey = pixels[width * i + j];
                int red = ((grey & 0x00FF0000) >> 16);
                int green = ((grey & 0x0000FF00) >> 8);
                int blue = (grey & 0x000000FF);
                grey = (int) (red * 0.3 + green * 0.59 + blue * 0.11);
                grey = alpha | (grey << 16) | (grey << 8) | grey;
                pixels[width * i + j] = grey;
            }
        }
        Bitmap newBmp = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        newBmp.setPixels(pixels, 0, width, 0, 0, width, height);
        return newBmp;
    }
}
