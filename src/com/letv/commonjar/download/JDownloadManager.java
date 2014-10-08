package com.letv.commonjar.download;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.letv.commonjar.CLog;
import com.letv.commonjar.download.JDownloadDBHelper.JDownloadTaskColumn;
import com.letv.commonjar.download.JDownloadDBHelper.JDownloadTaskColumnIndex;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Pair;

public class JDownloadManager {
	public static final String TAG = CLog.makeTag(JDownloadManager.class);

	public static class Request {
		private Uri mUri;
		private Uri mDataUri;
		private List<Pair<String, String>> mRequestHeaders = new ArrayList<Pair<String, String>>();
		private CharSequence mTitle;
		private CharSequence mDescription;
		/**
		 * This download is visible and shows in the notifications while in
		 * progress and after completion.
		 */
		public static final int VISIBILITY_VISIBLE_NOTIFY_COMPLETED = 1;
		public static final int VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION = 2;

		public Request(String uriString) {
			if (uriString == null || "".equals(uriString)) {
				throw new NullPointerException();
			}
			mUri = Uri.parse(uriString);
		}

		public Request(Uri uri) {
			if (uri == null) {
				throw new NullPointerException();
			}
			String scheme = uri.getScheme();
			if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
				throw new IllegalArgumentException("Can only download HTTP/HTTPS URIs: " + uri);
			}
			mUri = uri;
		}

		/**
		 * Add an HTTP header to be included with the download request. The
		 * header will be added to the end of the list.
		 * http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html
		 * 
		 * @param header
		 *            HTTP header name
		 * @param value
		 *            header value
		 * @return this object
		 */
		public Request addRequestHeader(String header, String value) {
			if (header == null) {
				throw new NullPointerException("header cannot be null");
			}
			if (header.contains(":")) {
				throw new IllegalArgumentException("header may not contain ':'");
			}
			if (value == null) {
				value = "";
			}
			mRequestHeaders.add(Pair.create(header, value));
			return this;
		}

		private void putIfNonNull(ContentValues contentValues, String key, Object value) {
			if (value != null) {
				contentValues.put(key, value.toString());
			}
		}

		public Request setDescription(CharSequence description) {
			mDescription = description;
			return this;
		}

		private void setDestinationFromBase(File base, String subPath) {
			if (subPath == null) {
				throw new NullPointerException("subPath cannot be null");
			}
			mDataUri = Uri.withAppendedPath(Uri.fromFile(base), subPath);
		}

		/**
		 * Set the local destination for the downloaded file to a path within
		 * the application's external files directory
		 * 
		 * @param context
		 * @param dirType
		 * @param subPath
		 * @return
		 */
		public Request setDestinationInExternalFilesDir(Context context, String dirType, String subPath) {
			setDestinationFromBase(context.getExternalFilesDir(dirType), subPath);
			return this;
		}

		/**
		 * Set the local destination for the downloaded file to a path within
		 * the public external storage directory
		 * 
		 * @param dirType
		 * @param subPath
		 * @return
		 */
		public Request setDestinationInExternalPublicDir(String dirType, String subPath) {
			File file = Environment.getExternalStoragePublicDirectory(dirType);
			if (file.exists()) {
				if (!file.isDirectory()) {
					throw new IllegalStateException(file.getAbsolutePath() + " already exists and is not a directory");
				}
			} else {
				if (!file.mkdir()) {
					throw new IllegalStateException("Unable to create directory: " + file.getAbsolutePath());
				}
			}
			setDestinationFromBase(file, subPath);
			return this;
		}

		/**
		 * Set the local destination for the downloaded file. Must be a file URI
		 * to a path on external storage, and the calling application must have
		 * the WRITE_EXTERNAL_STORAGE permission
		 * 
		 * @param uri
		 * @return
		 */
		public Request setDestinationUri(Uri uri) {
			mDataUri = uri;
			return this;
		}

		public Request setTitle(CharSequence title) {
			mTitle = title;
			return this;
		}

		/**
		 * @param packageName
		 * @return ContentValues to be passed to DownloadProvider.insert()
		 * @throws Exception
		 */
		ContentValues toContentValues(String className) throws Exception {
			ContentValues values = new ContentValues();
			assert mUri != null;
			values.put(JDownloadTaskColumn.URI, mUri.toString());
			if (className != null) {
				values.put(JDownloadTaskColumn.NOTIFICATION_CLASS, className);
			}
			if (mDataUri != null) {
				values.put(JDownloadTaskColumn.DATA, mDataUri.toString());
			} else {
				throw new Exception("mDataUri is null");
			}
			putIfNonNull(values, JDownloadTaskColumn.TITLE, mTitle);
			putIfNonNull(values, JDownloadTaskColumn.DESCRIPTION, mDescription);
			/**
			 * //TODO undo list : mRequestHeaders notify mode
			 */
			CLog.d(TAG, values.toString());
			values.put(JDownloadTaskColumn.STATUS, STATUS_QUEUEING);
			return values;
		}

		@Override
		public String toString() {
			return super.toString();
		}
	}

	public static final String ACTION_DOWNLOAD_TASK_ADD = "com.josyee.download.action_download_add";
	public static final String ACTION_DOWNLOAD_TASK_REMOVE = "com.josyee.download.action_download_remove";
	public static final String ACTION_DOWNLOAD_PROGRESS = "com.josyee.download.action_download_progress";
	public static final String ACTION_DOWNLOAD_COMPLETED = "com.josyee.download.action_download_completed";

	public static final String COLUMN_DOWNLOAD_ID = "_download_id";
	public final static String COLUMN_PROGRESS = "_download_progress";
	public final static String COLUMN_DESCRIPTION = "_download_description";
	public final static String COLUMN_STATUS = "_download_status";

	public final static int STATUS_QUEUEING = 0;
	public final static int STATUS_RUNNING = 1;
	public final static int STATUS_PAUSED = 2;
	public final static int STATUS_SUCCESSFUL = 3;
	public final static int STATUS_FAILED = 4;
	public final static int ERROR_UNKNOWN = 1000;

	private Context mCtx;
	private JDownloadDBUtils mDbUtils;
	private LocalBroadcastManager mLocalBroadcastManager;

	public JDownloadManager(Context ctx) {
		mCtx = ctx;
		mDbUtils = new JDownloadDBUtils(ctx);
		mLocalBroadcastManager = LocalBroadcastManager.getInstance(ctx);
		checkService();
	}

	public long addQueue(Request request) {
		long ret = -1;
		try {
			ContentValues values = request.toContentValues(null);
			ret = mDbUtils.insert(values);
			if (ret != -1) {
				notifyServiceAdd(ret);
			} else {
				throw new Exception("insert task failed");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

	public void remove(long id) {
		// delete database row in service
		notifyServiceRemove(id);
	}

	public void remove(long... ids) {
		// delete database row in service
		if (ids == null || ids.length == 0) {
		}
		notifyServiceRemove(ids[0]);
	}

	public Cursor queryTask(long id) {
		if (id == -1) {
			return null;
		}
		return mDbUtils.query(id);
	}

	public boolean isInExecute(long id) {
		boolean ret = false;
		Cursor cursor = queryTask(id);
		if (cursor != null && cursor.moveToFirst()) {
			int status = cursor.getInt(JDownloadTaskColumnIndex.STATUS);
			CLog.d(TAG, "isInExecute status=" + status);
			if (status != JDownloadManager.STATUS_SUCCESSFUL && status != JDownloadManager.STATUS_FAILED) {
				ret = true;
			}
			cursor.close();
		}
		return ret;
	}

	private void notifyServiceAdd(long id) {
		Intent intent = new Intent(ACTION_DOWNLOAD_TASK_ADD);
		intent.putExtra(COLUMN_DOWNLOAD_ID, id);
		mLocalBroadcastManager.sendBroadcast(intent);
	}

	private void notifyServiceRemove(long id) {
		Intent intent = new Intent(ACTION_DOWNLOAD_TASK_REMOVE);
		intent.putExtra(COLUMN_DOWNLOAD_ID, id);
		mLocalBroadcastManager.sendBroadcast(intent);
	}

	private void checkService() {
		boolean ret = false;
		int pid = android.os.Process.myPid();
		ActivityManager manager = (ActivityManager) mCtx.getSystemService(Context.ACTIVITY_SERVICE);
		ArrayList<RunningServiceInfo> runnings = (ArrayList<RunningServiceInfo>) manager
				.getRunningServices(Integer.MAX_VALUE);
		if (runnings != null) {
			for (RunningServiceInfo info : runnings) {
				//JLog.d(TAG, pid + "   name "+info.service.getClassName() + "  from "+info.pid +"  "+JDownloadService.class.getSimpleName());
				if (info.pid==pid && info.service.getClassName().toString().equals(JDownloadService.class.getSimpleName())) {
					ret = true;
					break;
				}
			}
		}
		if (!ret) {
			mCtx.startService(new Intent(mCtx, JDownloadService.class));
		}
	}
}
