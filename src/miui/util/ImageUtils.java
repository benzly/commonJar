package miui.util;

import android.graphics.Bitmap;

import com.letv.commonjar.CLog;

public class ImageUtils {

    static {
        CLog.d("ImageUtils", "load  imageutilities_jni.so  start");
        System.loadLibrary("imageutilities_jni");
        CLog.d("ImageUtils", "load  imageutilities_jni.so  end");
    }

    private static ImageUtils mImageUtils;

    public static ImageUtils getInstance() {
        if (mImageUtils == null) {
            synchronized (ImageUtils.class) {
                if (mImageUtils == null) {
                    mImageUtils = new ImageUtils();
                }
            }
        }
        return mImageUtils;
    }

    public void createFastBlur(Bitmap paramBitmap1, Bitmap paramBitmap2, int blurLevel) {
        native_fastBlur(paramBitmap1, paramBitmap2, blurLevel);
    }

    private native void native_fastBlur(Bitmap paramBitmap1, Bitmap paramBitmap2, int paramInt);
}
