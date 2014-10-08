package com.letv.commonjar.download;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class JDownloadThreadPool {

    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 3;
    private static final int KEEP_ALIVE_TIME = 2; // 10 seconds
    private final ThreadPoolExecutor mExecutor;

    public Executor getExecutor() {
        return mExecutor;
    }

    private static final JDownloadThreadPool instance = new JDownloadThreadPool();

    private JDownloadThreadPool() {
        mExecutor =
                new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS,
                        new LinkedBlockingQueue<Runnable>(), new JDownloadThreadFactory("thread-pool",
                                android.os.Process.THREAD_PRIORITY_LOWEST));
    }

    public static JDownloadThreadPool getInstance() {
        return instance;
    }

    public void submit(Runnable worker) {
        mExecutor.execute(worker);
    }

}
