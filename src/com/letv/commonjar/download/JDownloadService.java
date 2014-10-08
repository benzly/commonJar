package com.letv.commonjar.download;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.letv.commonjar.CLog;
import com.letv.commonjar.download.JDownloadDBHelper.JDownloadTaskColumnIndex;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

public class JDownloadService extends Service {
	private static final String TAG = CLog.makeTag(JDownloadService.class);

	private final class StartReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			long id = intent.getLongExtra(JDownloadManager.COLUMN_DOWNLOAD_ID, -1);
			if (JDownloadManager.ACTION_DOWNLOAD_TASK_ADD.equals(intent.getAction())) {
				CLog.d(TAG, "onReceive to add task " + id);
				addTask(id);
			} else if (JDownloadManager.ACTION_DOWNLOAD_TASK_REMOVE.equals(intent.getAction())) {
				CLog.d(TAG, "onReceive to remove task " + id);
				removeTask(id);
			}
		}

	}

	private static class Maps {
		public static <K, V> HashMap<K, V> newHashMap() {
			return new HashMap<K, V>();
		}
	}

	private StartReceiver mReceiver;
	private JDownloadDBUtils mDbUtils;
	private LocalBroadcastManager mLocalBroadcastManager;
	private Map<Long, JDownloadThread> mDownloads = Maps.newHashMap();

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Cannot bind to Download Service");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		CLog.d(TAG, "onCreate");
		mDbUtils = new JDownloadDBUtils(this);
		mReceiver = new StartReceiver();
		mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
		IntentFilter filter = new IntentFilter();
		filter.addAction(JDownloadManager.ACTION_DOWNLOAD_TASK_ADD);
		filter.addAction(JDownloadManager.ACTION_DOWNLOAD_TASK_REMOVE);
		mLocalBroadcastManager.registerReceiver(mReceiver, filter);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		CLog.d(TAG, "onStartCommand");
		int ret = super.onStartCommand(intent, flags, startId);
		Cursor cursor = mDbUtils.queryAll();
		if (cursor != null) {
			while (cursor.moveToNext()) {
				long id = cursor.getLong(JDownloadTaskColumnIndex.ID);
				int status = cursor.getInt(JDownloadTaskColumnIndex.STATUS);
				if (!mDownloads.containsKey(id)) {
					if (status == JDownloadManager.STATUS_QUEUEING || status == JDownloadManager.STATUS_RUNNING //
							|| status == JDownloadManager.STATUS_PAUSED) {
						JDownloadInfo info = new JDownloadInfo();
						info.mId = id;
						info.mStatus = status;
						info.mTitle = cursor.getString(JDownloadTaskColumnIndex.TITLE);
						info.mUri = cursor.getString(JDownloadTaskColumnIndex.URI);
						info.mData = cursor.getString(JDownloadTaskColumnIndex.DATA);
						info.mCurrentBytes = cursor.getLong(JDownloadTaskColumnIndex.CURRENT_BYTES);
						info.mTotalBytes = cursor.getLong(JDownloadTaskColumnIndex.TOTAL_BYTES);
						info.mDescription = cursor.getString(JDownloadTaskColumnIndex.DESCRIPTION);
						JDownloadThread t = new JDownloadThread(JDownloadService.this, info);
						JDownloadThreadPool.getInstance().submit(t);
						mDownloads.put(id, t);
						CLog.d(TAG, "resume task = "+info.mId +"   "+info.mTitle);
					}
				}
			}
		}
		return ret;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mLocalBroadcastManager.unregisterReceiver(mReceiver);
	}

	public void addTask(long id) {
		if (!mDownloads.containsKey(id)) {
			CLog.d(TAG, "addTask " + id);
			Cursor cursor = mDbUtils.query(id);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					JDownloadInfo info = new JDownloadInfo();
					info.mId = cursor.getLong(JDownloadTaskColumnIndex.ID);
					info.mStatus = cursor.getInt(JDownloadTaskColumnIndex.STATUS);
					info.mUri = cursor.getString(JDownloadTaskColumnIndex.URI);
					info.mTitle = cursor.getString(JDownloadTaskColumnIndex.TITLE);
					info.mData = cursor.getString(JDownloadTaskColumnIndex.DATA);
					info.mDescription = cursor.getString(JDownloadTaskColumnIndex.DESCRIPTION);
					info.mCurrentBytes = cursor.getLong(JDownloadTaskColumnIndex.CURRENT_BYTES);
					info.mTotalBytes = cursor.getLong(JDownloadTaskColumnIndex.TOTAL_BYTES);
					CLog.d(TAG, info.toString());
					JDownloadThread t = new JDownloadThread(this, info);
					JDownloadThreadPool.getInstance().submit(t);
					mDownloads.put(id, t);
				}
				cursor.close();
			}
		}
	}

	public void removeTask(long id) {
		if (mDownloads.containsKey(id)) {
			JDownloadThread t = mDownloads.remove(id);
			t.breakTask();
			t.interrupt();
			deleteTaskFile(id);
			mDbUtils.delete(id);
			CLog.d(TAG, "removeTask " + id);
		}
	}

	private void deleteTaskFile(long id) {
		Cursor c = mDbUtils.query(id);
		if (c != null && c.moveToFirst()) {
			String path = c.getString(JDownloadTaskColumnIndex.DATA);
			File file = new File(path);
			if (file.exists()) {
				CLog.d(TAG, "removeFile " + file.getName());
				file.delete();
			}
		}
	}
}
