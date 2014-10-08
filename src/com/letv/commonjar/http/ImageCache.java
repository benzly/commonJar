/**
 * =====================================================================
 *
 * @file ImageCache.java
 * @Module Name com.nj.common.utils.cache
 * @author benz
 * @OS version 1.0
 * @Product type: JoySee
 * @date 2013-12-5
 * @brief This file is the http **** implementation.
 * @This file is responsible by ANDROID TEAM.
 * @Comments: ===================================================================== Revision
 *            History:
 *
 *            Modification Tracking
 *
 *            Author Date OS version Reason ---------- ------------ ------------- ----------- benz
 *            2013-12-5 1.0 Check for NULL, 0 h/w
 *            =====================================================================
 **/
//

package com.letv.commonjar.http;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.util.LruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Iterator;

import com.letv.commonjar.CLog;

class ImageCache {

    private static final String TAG = CLog.makeTag(ImageCache.class);

    private CacheParams mJCacheParams;
    private DiskLruCache mDiskLruCache;
    private boolean mDiskCacheStarting = true;
    private static final int DISK_CACHE_INDEX = 0;
    private final Object mDiskCacheLock = new Object();
    private final Object mHttpDiskCacheLock = new Object();
    private LruCache<String, BitmapDrawable> mMemoryCache;
    // 存放被回收的bitmap
    private HashSet<SoftReference<Bitmap>> mReusableBitmaps;
    private static final CompressFormat DEFAULT_COMPRESS_FORMAT = CompressFormat.JPEG;

    ImageCache(Context ctx, CacheParams params) {
        this.mJCacheParams = params;
        init(params);
    }

    private void init(CacheParams params) {

        if (mJCacheParams.isUseMemoryCache()) {
            mReusableBitmaps = new HashSet<SoftReference<Bitmap>>();
            mMemoryCache = new LruCache<String, BitmapDrawable>(mJCacheParams.getMemorySize()) {
                @Override
                protected void entryRemoved(boolean evicted, String key, BitmapDrawable oldValue, BitmapDrawable newValue) {
                    /** 硬引用缓存区满，将一个最不经常使用的OldValue推入到软引用缓存区 */
                    mReusableBitmaps.add(new SoftReference<Bitmap>(oldValue.getBitmap()));
                }

                @Override
                protected int sizeOf(String key, BitmapDrawable value) {
                    final int bitmapSize = getBitmapSize(value) / 1024;
                    return bitmapSize == 0 ? 1 : bitmapSize;
                }
            };
        }
    }

    /**
     * this should not be executed on the main/UI thread.
     */
    public void initDiskCache() {

        synchronized (mDiskCacheLock) {
            if (mDiskLruCache == null || mDiskLruCache.isClosed()) {
                File diskCacheDir = mJCacheParams.getDisCacheDirFile();
                if (mJCacheParams.isUseDiskCache() && diskCacheDir != null) {
                    if (!diskCacheDir.exists()) {
                        diskCacheDir.mkdirs();
                    }
                    if (getUsableSpace(diskCacheDir) > mJCacheParams.getDiskSize()) {
                        try {
                            mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, mJCacheParams.getDiskSize());
                        } catch (IOException e) {
                            CLog.e(TAG, "DiskCache open Exception");
                        }
                    } else {
                        CLog.e(TAG, "Disk space is not enough");
                    }
                }
            }
            mDiskCacheStarting = false;
            mDiskCacheLock.notifyAll();
        }
    }

    public void addBitmapToCache(String data, BitmapDrawable value) {
        if (data == null || value == null) {
            return;
        }
        if (mMemoryCache != null) {
            mMemoryCache.put(data, value);
        }
        synchronized (mDiskCacheLock) {
            if (mDiskLruCache != null) {
                final String key = hashKeyForDisk(data);
                OutputStream out = null;
                try {
                    DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
                    if (snapshot == null) {
                        final DiskLruCache.Editor editor = mDiskLruCache.edit(key);
                        if (editor != null) {
                            out = editor.newOutputStream(DISK_CACHE_INDEX);
                            value.getBitmap().compress(DEFAULT_COMPRESS_FORMAT, mJCacheParams.getJpgSaveQuality(), out);
                            editor.commit();
                            out.close();
                        }
                    } else {
                        snapshot.getInputStream(DISK_CACHE_INDEX).close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {}
                }
            }
        }
    }

    public BitmapDrawable getBitmapFromMemCache(String key) {
        BitmapDrawable memValue = null;
        if (mMemoryCache != null) {
            memValue = mMemoryCache.get(key);
        }
        return memValue;
    }

    public Bitmap getBitmapFromDiskCache(String data, int[] size) {
        final String key = hashKeyForDisk(data);
        Bitmap bitmap = null;
        synchronized (mDiskCacheLock) {
            while (mDiskCacheStarting) {
                try {
                    mDiskCacheLock.wait();
                } catch (InterruptedException e) {}
            }
            if (mDiskLruCache != null) {
                InputStream inputStream = null;
                try {
                    final DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
                    if (snapshot != null) {
                        inputStream = snapshot.getInputStream(DISK_CACHE_INDEX);
                        if (inputStream != null) {
                            FileDescriptor fd = ((FileInputStream) inputStream).getFD();
                            bitmap = decodeBitmapFromDescriptor(fd, size);
                        }
                    }
                } catch (IOException e) {
                    CLog.e(TAG, "getBitmapFromDiskCache e=" + e);
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (IOException e) {}
                }
            }
            return bitmap;
        }
    }

    /**
     * @param options - BitmapFactory.Options with out* options populated
     * @return Bitmap that case be used for inBitmap
     */
    protected Bitmap getBitmapFromReusableSet(BitmapFactory.Options options) {

        Bitmap bitmap = null;
        if (mReusableBitmaps != null && !mReusableBitmaps.isEmpty()) {
            final Iterator<SoftReference<Bitmap>> iterator = mReusableBitmaps.iterator();
            Bitmap item = null;
            while (iterator.hasNext()) {
                SoftReference<Bitmap> softReference = iterator.next();
                item = softReference.get();
                if (null != item && item.isMutable()) {
                    /** Check to see it the item can be used for inBitmap */
                    if (canUseForInBitmap(item, options)) {
                        bitmap = item;
                        iterator.remove();
                        break;
                    }
                } else {
                    iterator.remove();
                }
            }
        }

        return bitmap;
    }

    /**
     * this should not be executed on the main/UI thread.
     */
    public void clearCache() {
        CLog.d(TAG, " - cache cleared - ");

        if (mMemoryCache != null) {
            mMemoryCache.evictAll();
        }

        synchronized (mDiskCacheLock) {
            mDiskCacheStarting = true;
            if (mDiskLruCache != null && !mDiskLruCache.isClosed()) {
                try {
                    mDiskLruCache.delete();
                } catch (IOException e) {}
                mDiskLruCache = null;
                // initDiskCache();
            }
        }
    }

    /**
     * this should not be executed on the main/UI thread.
     */
    public void flush() {
        synchronized (mDiskCacheLock) {
            if (mDiskLruCache != null) {
                try {
                    mDiskLruCache.flush();
                    CLog.d(TAG, " - Disk cache flushed - flush");
                } catch (IOException e) {
                    CLog.d(TAG, "flush - " + e);
                }
            }
        }
    }

    /**
     * this should not be executed on the main/UI thread.
     */
    public void close() {
        log(TAG, "Disk cache closed");
        synchronized (mDiskCacheLock) {
            if (mDiskLruCache != null && !mDiskLruCache.isClosed()) {
                try {
                    mDiskLruCache.close();
                    mDiskLruCache = null;
                } catch (IOException e) {}
            }
        }
    }

    private static boolean canUseForInBitmap(Bitmap candidate, BitmapFactory.Options targetOptions) {
        int width = targetOptions.outWidth / targetOptions.inSampleSize;
        int height = targetOptions.outHeight / targetOptions.inSampleSize;
        return candidate.getWidth() == width && candidate.getHeight() == height;
    }

    public static File getDiskCacheDir(Context context, String uniqueName) {
        final String cachePath =
                Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !isExternalStorageRemovable()
                        ? getExternalCacheDir(context).getPath()
                        : context.getCacheDir().getPath();

        return new File(cachePath + File.separator + uniqueName);
    }

    /**
     * A hashing method that changes a string (like a URL) into a hash suitable for using as a disk
     * filename.
     */
    public static String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    private static String bytesToHexString(byte[] bytes) {
        // http://stackoverflow.com/questions/332079
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * Get the size in bytes of a bitmap in a BitmapDrawable.
     * 
     * @param value
     * @return size in bytes
     */
    public static int getBitmapSize(BitmapDrawable value) {
        Bitmap bitmap = value.getBitmap();

        if (CacheUtils.hasHoneycombMR1()) {
            return bitmap.getByteCount();
        }
        // Pre HC-MR1
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    /**
     * Check if external storage is built-in or removable.
     */
    public static boolean isExternalStorageRemovable() {
        if (CacheUtils.hasGingerbread()) {
            return Environment.isExternalStorageRemovable();
        }
        return true;
    }

    /**
     * Get the external app cache directory.
     * 
     * @param context The context to use
     * @return The external cache dir
     */
    public static File getExternalCacheDir(Context context) {
        if (CacheUtils.hasFroyo()) {
            return context.getExternalCacheDir();
        }

        // Before Froyo we need to construct the external cache dir ourselves
        final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
        return new File(Environment.getExternalStorageDirectory().getPath() + cacheDir);
    }

    /**
     * 检查可用空间
     */
    public static long getUsableSpace(File path) {
        if (CacheUtils.hasGingerbread()) {
            return path.getUsableSpace();
        }
        final StatFs stats = new StatFs(path.getPath());
        return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
    }

    protected Bitmap processBitmap(String bitmapUrl, int[] size) {
        final String key = ImageCache.hashKeyForDisk(bitmapUrl);
        FileDescriptor fileDescriptor = null;
        FileInputStream fileInputStream = null;
        Bitmap bitmap = null;
        DiskLruCache.Snapshot snapshot;
        synchronized (mHttpDiskCacheLock) {
            while (mDiskCacheStarting) {
                try {
                    mHttpDiskCacheLock.wait();
                } catch (InterruptedException e) {}
            }

            if (mDiskLruCache != null) {
                try {
                    snapshot = mDiskLruCache.get(key);
                    if (null == snapshot) {
                        DiskLruCache.Editor editor = mDiskLruCache.edit(key);
                        if (editor != null) {
                            if (downloadUrlToStream(bitmapUrl, editor.newOutputStream(DISK_CACHE_INDEX))) {
                                editor.commit();
                            } else {
                                editor.abort();
                            }
                        }
                        snapshot = mDiskLruCache.get(key);
                    }
                    if (null != snapshot) {
                        fileInputStream = (FileInputStream) snapshot.getInputStream(DISK_CACHE_INDEX);
                        fileDescriptor = fileInputStream.getFD();
                    }
                } catch (IOException e) {
                    CLog.e(TAG, "downBitmap - " + e);
                } catch (IllegalStateException e) {
                    CLog.e(TAG, "downBitmap - " + e);
                } finally {
                    if (fileDescriptor == null && fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e) {}
                    }
                }
            }
        }
        if (null != fileDescriptor) {
            bitmap = decodeBitmapFromDescriptor(fileDescriptor, size);
        }
        if (fileInputStream != null) {
            try {
                fileInputStream.close();
            } catch (IOException e) {}
        }
        return bitmap;
    }

    /**
     * decode bitmap from FileDescriptor
     * 
     * @param descriptor
     */
    private Bitmap decodeBitmapFromDescriptor(FileDescriptor descriptor, int[] size) {
        if (size == null) {
            return BitmapFactory.decodeFileDescriptor(descriptor);
        } else {
            return decodeBitmapFromDescriptorByWH(descriptor, size[0], size[1]);
        }
    }

    /**
     * Decode and sample down a bitmap from a file input stream to the requested width and height.
     * 
     * @param descriptor
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private Bitmap decodeBitmapFromDescriptorByWH(FileDescriptor descriptor, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(descriptor, null, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        addInBitmapOptions(options);
        return zoomBitmap(BitmapFactory.decodeFileDescriptor(descriptor, null, options), reqWidth, reqHeight);
    }

    private void addInBitmapOptions(BitmapFactory.Options options) {
        options.inMutable = true;
        Bitmap inBitmap = getBitmapFromReusableSet(options);
        if (null != inBitmap) {
            options.inBitmap = inBitmap;
        }
    }

    /**
     * Calculate an inSampleSize
     */
    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
            final float totalPixels = width * height;
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;
            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
    }

    private Bitmap zoomBitmap(Bitmap bitmap, int width, int height) {
        if (bitmap == null) {
            return null;
        }
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        if (w <= 0 || h <= 0) {
            return bitmap;
        }

        Matrix matrix = new Matrix();
        float scaleWidth = ((float) width / w);
        float scaleHeight = ((float) height / h);
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap newbmp = Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
        bitmap.recycle();
        return newbmp;
    }

    public boolean downloadUrlToStream(String imagePath, OutputStream outputStream) {
        boolean ret = false;
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;
        try {
            final URL url = new URL(imagePath);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(), 8 * 1024);
            out = new BufferedOutputStream(outputStream, 8 * 1024);
            int b = -1;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            ret = true;
        } catch (final IOException e) {
            log(TAG, "- Error in download img - " + e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (final IOException e) {}
        }
        return ret;
    }

    private static void log(String tag, String msg) {
        if (CLog.MANUAL_POWER) {
            CLog.d(tag, msg);
        }
    }

}
