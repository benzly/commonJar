/**
 * =====================================================================
 *
 * @file ImageProvider.java
 * @Module Name com.nj.common.utils.cache
 * @author benz
 * @OS version 1.0
 * @Product type: JoySee
 * @date 2013-12-4
 * @brief This file is the http **** implementation.
 * @This file is responsible by ANDROID TEAM.
 * @Comments: ===================================================================== Revision
 *            History:
 *
 *            Modification Tracking
 *
 *            Author Date OS version Reason ---------- ------------ ------------- ----------- benz
 *            2013-12-4 1.0 Check for NULL, 0 h/w
 *            =====================================================================
 **/
//

package com.letv.commonjar.http;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import com.letv.commonjar.CLog;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;

class ImageProvider {

    private static final String TAG = CLog.makeTag(ImageProvider.class);

    private static final int MSG_CLEAR = 0;
    private static final int MSG_FLUSH = 1;
    private static final int MSG_CLOSE = 2;
    private static final int MSG_INIT_DISK = 3;

    private Resources mResources;

    private boolean mExitWork = false;
    private boolean mPauseWork = false;
    private final Object mPauseWorkLock = new Object();

    private static ImageProvider mProviderInstance;
    private static ImageCache mImageCache;
    private final Map<String, WeakReference<Future<?>>> mProcessTaskMap;

    private ImageProvider(Context ctx, CacheParams params) {
        params = params == null ? new CacheParams(ctx) : params;
        mImageCache = new ImageCache(ctx, params);
        mResources = ctx.getResources();
        mProcessTaskMap = new HashMap<String, WeakReference<Future<?>>>();
        new CacheAsyncTask().execute(MSG_INIT_DISK);
    }

    public static ImageProvider getInstance(Context ctx, CacheParams params) {
        if (mProviderInstance == null) {
            synchronized (ImageProvider.class) {
                if (mProviderInstance == null) {
                    mProviderInstance = new ImageProvider(ctx, params);
                }
            }
        }
        return mProviderInstance;
    }

    public void flush() {
        new CacheAsyncTask().execute(MSG_FLUSH);
    }

    public void close() {
        new CacheAsyncTask().execute(MSG_CLOSE);
    }

    public void getBitmap(String key, FetchBackListener fb) {
        if (key == null || mImageCache == null || fb == null) {
            return;
        }
        BitmapDrawable memoryDrawable = mImageCache.getBitmapFromMemCache(key);
        if (memoryDrawable != null) {
            fb.fetchSuccess(key, memoryDrawable);
            return;
        }

        BitmapWorkerTask workerTask = new BitmapWorkerTask(key, fb);
        workerTask.executeOnExecutor(AsyncTask.DUAL_THREAD_EXECUTOR, key);
        Future<?> future = workerTask.getFuture();
        if (future != null) {
            mProcessTaskMap.put(key, new WeakReference<Future<?>>(future));
        }
    }

    public void getBitmap(String key, int[] size, FetchBackListener fb) {
        if (key == null || mImageCache == null || fb == null) {
            return;
        }
        BitmapDrawable memoryDrawable = mImageCache.getBitmapFromMemCache(key);
        if (memoryDrawable != null) {
            if (size != null) {
                if (memoryDrawable.getBitmap().getWidth() == size[0] && memoryDrawable.getBitmap().getHeight() == size[1]) {
                    fb.fetchSuccess(key, memoryDrawable);
                    return;
                }
            }
        }
        BitmapWorkerTask workerTask = new BitmapWorkerTask(key, size, fb);
        workerTask.executeOnExecutor(AsyncTask.DUAL_THREAD_EXECUTOR, key);
        Future<?> future = workerTask.getFuture();
        if (future != null) {
            mProcessTaskMap.put(key, new WeakReference<Future<?>>(future));
        }
    }

    public void loadImage(String key, ImageView imageView) {

    }

    public void cancelTask(String url, boolean mayInterruptIfRunning) {
        synchronized (mProcessTaskMap) {
            WeakReference<Future<?>> futureWR = mProcessTaskMap.get(url);
            if (futureWR != null) {
                Future<?> request = futureWR.get();
                if (request != null) {
                    request.cancel(mayInterruptIfRunning);
                }
                mProcessTaskMap.remove(url);
            }
        }
    }

    public void cancelTask(ArrayList<String> urls, boolean mayInterruptIfRunning) {
        if (urls == null) {
            return;
        }
        synchronized (mProcessTaskMap) {
            int size = urls.size();
            long begin = CLog.methodBegin(TAG);
            for (int i = 0; i < size; i++) {
                String url = urls.get(i);
                WeakReference<Future<?>> futureWR = mProcessTaskMap.get(url);
                if (futureWR != null) {
                    Future<?> request = futureWR.get();
                    if (request != null) {
                        request.cancel(mayInterruptIfRunning);
                    }
                    mProcessTaskMap.remove(url);
                }
            }
            CLog.methodEnd(TAG, begin, size + " count");
        }
    }

    public void pauseWork() {
        mPauseWork = true;
    }

    public void continueWork() {
        mPauseWork = false;
    }

    public void exitCacheCient() {
        mExitWork = true;
        new CacheAsyncTask().execute(MSG_CLOSE);
    }

    private class BitmapWorkerTask extends AsyncTask<Object, Void, BitmapDrawable> {

        private String key;
        private int[] size;
        private FetchBackListener fb;

        public BitmapWorkerTask(String key, FetchBackListener fb) {
            this.key = key;
            this.fb = fb;
        }

        public BitmapWorkerTask(String key, int[] size, FetchBackListener fb) {
            this.key = key;
            this.fb = fb;
            this.size = size;
        }

        @Override
        protected BitmapDrawable doInBackground(Object... params) {
            key = String.valueOf(params[0]);
            Bitmap bitmap = null;
            BitmapDrawable drawable = null;
            synchronized (mPauseWorkLock) {
                while (mPauseWork && !isCancelled()) {
                    try {
                        mPauseWorkLock.wait();
                    } catch (InterruptedException e) {}
                }
            }
            /**
             * get from disk
             */
            if (mImageCache != null && !mExitWork) {
                bitmap = mImageCache.getBitmapFromDiskCache(key, size);
            }
            /**
             * get from net
             */
            if (bitmap == null && !isCancelled() && !mExitWork) {
                bitmap = mImageCache.processBitmap(key, size);
            }
            /**
             * add to cache
             */
            if (bitmap != null) {
                drawable = new BitmapDrawable(mResources, bitmap);
                if (mImageCache != null) {
                    mImageCache.addBitmapToCache(key, drawable);
                }
            }
            return drawable;
        }

        @Override
        protected void onPostExecute(BitmapDrawable value) {
            if (isCancelled() || mExitWork) {
                value = null;
            }
            if (fb != null) {
                fb.fetchSuccess(key, value);
            }
        }

        @Override
        protected void onCancelled(BitmapDrawable value) {
            super.onCancelled(value);
            synchronized (mPauseWorkLock) {
                mPauseWorkLock.notifyAll();
            }
        }
    }

    protected class CacheAsyncTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            if (mImageCache != null) {
                switch ((Integer) params[0]) {
                    case MSG_CLEAR:
                        mImageCache.clearCache();
                        break;
                    case MSG_FLUSH:
                        mImageCache.flush();
                        break;
                    case MSG_CLOSE:
                        mImageCache.close();
                        break;
                    case MSG_INIT_DISK:
                        mImageCache.initDiskCache();
                        break;
                }
            }
            return null;
        }

    }

}
