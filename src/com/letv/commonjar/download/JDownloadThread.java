package com.letv.commonjar.download;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import com.letv.commonjar.CLog;
import com.letv.commonjar.download.JDownloadDBHelper.JDownloadTaskColumn;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.http.AndroidHttpClient;
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

public class JDownloadThread extends Thread {
    private static final String TAG = CLog.makeTag(JDownloadThread.class);

    static class State {
        public long id;
        public String url;
        public int status;
        public long totalBytes;
        public long currentBytes;
        public String targetFile;
        public String description;
        public int mProgress;
        public long mBytesNotified = 0;
        public long mTimeLastNotified = 0;
        public boolean mContinuingDownload;
        public FileOutputStream outputStream;

        State(JDownloadInfo info) {
            this.id = info.mId;
            this.url = info.mUri;
            this.targetFile = info.mData;
            this.currentBytes = info.mCurrentBytes;
            this.totalBytes = info.mTotalBytes;
            this.description = info.mDescription;
            this.status = info.mStatus;
        }
    }

    private Context mCtx;
    private Date mBeginDate;
    private JDownloadInfo mInfo;
    private JDownloadDBUtils mDbUtils;
    private LocalBroadcastManager mBroadcastManager;

    private boolean isStop;

    public JDownloadThread(Context context, JDownloadInfo info) {
        this.mCtx = context;
        this.mInfo = info;
        this.mDbUtils = new JDownloadDBUtils(context);
        this.mBroadcastManager = LocalBroadcastManager.getInstance(context);
    }

    private boolean checkConnectivity() {
        return true;
    }

    /**
     * 执行断点下载
     * 
     * @param state
     * @param httpGet
     */
    private void handlerContinueDownload(State state, HttpGet httpGet) throws FileNotFoundException {
        if (TextUtils.isEmpty(state.targetFile)) {
            throw new FileNotFoundException();
        }
        File target = new File(state.targetFile);
        File targetParent = new File(target.getParent());
        if (!targetParent.exists()) {
            targetParent.mkdir();
        }
        if (target.exists()) {
            long targetLength = target.length();
            if (targetLength == 0) {
                target.delete();
                state.currentBytes = 0;
                CLog.d(TAG, "handler continue download, but old content length is 0, so to download new again");
            } else {
                httpGet.setHeader("Range", "bytes=" + targetLength + "-");
                state.outputStream = new FileOutputStream(state.targetFile, true);
                state.currentBytes = targetLength;
                state.mContinuingDownload = true;
                CLog.d(TAG, "handlerContinueDownload  currentBytes=" + state.currentBytes + "  totalBytes=" + state.totalBytes);
            }
        }
    }

    private void handlerDownloadPrepare(State state, HttpResponse response) throws FileNotFoundException, JDownloadError.PrepareException {
        int responseCode = response.getStatusLine().getStatusCode();
        long contentLength = response.getEntity().getContentLength();
        CLog.d(TAG, "processPrepare  response code=" + responseCode + "  service content length = " + contentLength);
        if (contentLength <= 0) {
            throw new JDownloadError.PrepareException("processPrepare file size <= 0");
        }
        // 不支持断点续传
        if (state.mContinuingDownload && responseCode != 206) {
            state.mContinuingDownload = false;
            if (state.outputStream != null) {
                try {
                    state.outputStream.close();
                    state.outputStream = null;
                } catch (Exception e) {}
            }
            CLog.e(TAG, "handlerDownloadPrepare ---> web server is not support 206");
        }
        if (!state.mContinuingDownload) {
            state.outputStream = new FileOutputStream(state.targetFile);
            state.currentBytes = 0;
            state.totalBytes = contentLength;
        }
        ContentValues values = new ContentValues();
        values.put(JDownloadTaskColumn.STATUS, state.status);
        values.put(JDownloadTaskColumn.CURRENT_BYTES, state.currentBytes);
        values.put(JDownloadTaskColumn.TOTAL_BYTES, state.totalBytes);
        mDbUtils.update(state.id, values);
    }

    /**
     * 按间隔写入database
     * 
     * @param state
     */
    private void brushDatabase(State state) {
        long now = System.currentTimeMillis();
        if (state.currentBytes - state.mBytesNotified > JConstants.MIN_PROGRESS_STEP //
                && now - state.mTimeLastNotified > JConstants.MIN_PROGRESS_TIME) {
            ContentValues values = new ContentValues();
            values.put(JDownloadTaskColumn.CURRENT_BYTES, state.currentBytes);
            mDbUtils.update(state.id, values);
            state.mBytesNotified = state.currentBytes;
        }
        reportProgress(state);
    }

    /**
     * 进度增加>=1, notify一次
     * 
     * @param state
     */
    private void reportProgress(State state) {
        if (state.totalBytes > 0) {
            int temp = (int) (state.currentBytes * 100 / state.totalBytes);
            if (temp - state.mProgress >= 1) {
                state.mProgress = temp;
                Intent intent = new Intent(JDownloadManager.ACTION_DOWNLOAD_PROGRESS);
                intent.putExtra(JDownloadManager.COLUMN_DOWNLOAD_ID, state.id);
                intent.putExtra(JDownloadManager.COLUMN_DESCRIPTION, state.description);
                intent.putExtra(JDownloadManager.COLUMN_PROGRESS, state.mProgress);
                mBroadcastManager.sendBroadcast(intent);
                CLog.d(TAG, "reportProgress   task id=" + mInfo.mId + "  " + state.mProgress);
            }
        }
    }

    private void handlerDownloadEnd(State state) {
        long usetime = new Date(System.currentTimeMillis()).getTime() - mBeginDate.getTime();
        ContentValues values = new ContentValues();
        values.put(JDownloadTaskColumn.TIME_USE, usetime);
        values.put(JDownloadTaskColumn.STATUS, state.status);
        values.put(JDownloadTaskColumn.CURRENT_BYTES, state.currentBytes);
        mDbUtils.update(mInfo.mId, values);
    }

    private void notifyDownloadCompleted(State state) {
        Intent intent = new Intent(JDownloadManager.ACTION_DOWNLOAD_COMPLETED);
        intent.putExtra(JDownloadManager.COLUMN_DOWNLOAD_ID, state.id);
        intent.putExtra(JDownloadManager.COLUMN_DESCRIPTION, state.description);
        intent.putExtra(JDownloadManager.COLUMN_STATUS, state.status);
        mBroadcastManager.sendBroadcast(intent);
        clean(state);
    }

    private void clean(State state) {
        if (state.status != JDownloadManager.STATUS_SUCCESSFUL) {
            File f = new File(state.targetFile);
            if (f != null) {
                f.delete();
            }
        }
    }

    private String statusToString(int status, String errorMsg) {
        StringBuilder sb = new StringBuilder();
        switch (status) {
            case JDownloadManager.STATUS_SUCCESSFUL:
                sb.append("STATUS_SUCCESSFUL");
                break;
            case JDownloadManager.STATUS_FAILED:
                sb.append("STATUS_FAILED");
                sb.append("---" + errorMsg);
                break;
            default:
                sb.append("STATUS_FAILED");
                sb.append("---" + errorMsg);
                break;
        }
        return sb.toString();
    }

    public void breakTask() {
        isStop = true;
    }

    @Override
    public void run() {
        CLog.d(TAG, "JDownloadThread run " + mInfo.mId + "  " + mInfo.mUri);
        if (isStop) {
            return;
        }
        Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
        mBeginDate = new Date(System.currentTimeMillis());
        State state = new State(mInfo);
        state.status = JDownloadManager.STATUS_RUNNING;
        StringBuilder sb = new StringBuilder();

        HttpGet httpGet = null;
        InputStream inputStream = null;
        AndroidHttpClient httpClient = null;
        try {
            if (!checkConnectivity()) {
                throw new JDownloadError.NetTimeOut("net error");
            }
            httpGet = new HttpGet(mInfo.mUri);
            httpClient = AndroidHttpClient.newInstance(TAG, mCtx);
            // 处理是否可以断点续传
            handlerContinueDownload(state, httpGet);
            // 获取响应
            HttpResponse httpResponse = httpClient.execute(httpGet);
            // 检测服务器端文件，并更新数据库
            handlerDownloadPrepare(state, httpResponse);
            // 获取文件流
            inputStream = httpResponse.getEntity().getContent();

            int offset = 0;
            byte buffer[] = new byte[4096];
            while (!isStop && (offset = inputStream.read(buffer)) != -1) {
                state.outputStream.write(buffer, 0, offset);
                state.currentBytes += offset;
                brushDatabase(state);
            }
            if (state.currentBytes == state.totalBytes) {
                state.status = JDownloadManager.STATUS_SUCCESSFUL;
                handlerDownloadEnd(state);
            } else {
                state.status = JDownloadManager.STATUS_FAILED;
                sb.append("down size not match");
            }

        } catch (JDownloadError.NetTimeOut e) {
            sb.append("NetTimeOut");
            state.status = JDownloadManager.STATUS_FAILED;
        } catch (FileNotFoundException e) {
            sb.append("FileNotFoundException");
            state.status = JDownloadManager.STATUS_FAILED;
        } catch (JDownloadError.PrepareException e) {
            sb.append("PrepareException");
            state.status = JDownloadManager.STATUS_FAILED;
        } catch (IOException e) {
            sb.append("IOException");
            state.status = JDownloadManager.STATUS_FAILED;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                inputStream = null;
            }
            if (state.outputStream != null) {
                try {
                    state.outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                state.outputStream = null;
            }
            if (httpClient != null) {
                httpClient.close();
                httpClient = null;
            }
            if (httpGet != null) {
                httpGet.abort();
                httpGet = null;
            }
            CLog.d(TAG, "run finally  state = " + statusToString(state.status, sb.toString()));
            if (!isStop) {
                notifyDownloadCompleted(state);
            }
        }
    }

}
