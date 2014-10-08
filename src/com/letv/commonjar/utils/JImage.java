/**
 * =====================================================================
 *
 * @file  JImage.java
 * @Module Name   com.joysee.common.utils
 * @author benz
 * @OS version  1.0
 * @Product type: JoySee
 * @date   2013-12-17
 * @brief  This file is the http **** implementation.
 * @This file is responsible by ANDROID TEAM.
 * @Comments: 
 * =====================================================================
 * Revision History:
 *
 *                   Modification  Tracking
 *
 * Author            Date            OS version        Reason 
 * ----------      ------------     -------------     -----------
 * benz          2013-12-17           1.0         Check for NULL, 0 h/w
 * =====================================================================
 **/

package com.letv.commonjar.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;

public class JImage {

    /**
     * @param originalImage 原图
     * @param invertedImageH 倒影高度
     * @return
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
     * 获取圆角图片
     * 
     * @param bitmap
     * @param roundPx
     * @return
     */
    public static Bitmap getRound(Bitmap bitmap, float roundPx) {
        if (bitmap == null) {
            return null;
        }
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Config.ARGB_8888);
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
     * 获取倒影加圆角图片
     * 
     * @param originalImage 原图
     * @param invertedImageH 倒影高度
     * @param roundPx 圆角角度
     * @return
     */
    public static Bitmap getRoundReflect(Bitmap originalImage, int invertedImageH, float roundPx) {
        return getRound(getReflect(originalImage, invertedImageH, true), roundPx);
    }
}
