package com.letv.commonjar.download;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.letv.commonjar.CLog;

public class JDownloadDBHelper extends SQLiteOpenHelper {

	private static final String TAG = CLog.makeTag(JDownloadDBHelper.class);

	public final static String DB_NAME = "jdownload.db";
	public final static String DOWNLOAD_TABLE = "jdownloads";
	public final static int DB_VERSION = 2;
	private static JDownloadDBHelper mJHelper;
	private static String CREATE_DOWNLOAD_TABLE;
	
	static {
		CREATE_DOWNLOAD_TABLE = new StringBuilder()
		.append("CREATE TABLE IF NOT EXISTS " + DOWNLOAD_TABLE +" (")
		.append(JDownloadTaskColumn.ID +" INTEGER primary key autoincrement ,")
		.append(JDownloadTaskColumn.URI + " TEXT ,")
		.append(JDownloadTaskColumn.DATA + " TEXT ,")
		.append(JDownloadTaskColumn.STATUS + " INTEGER ,")
		.append(JDownloadTaskColumn.NOTIFICATION_CLASS + " TEXT ,")
		.append(JDownloadTaskColumn.TOTAL_BYTES + " INTEGER ,")
		.append(JDownloadTaskColumn.CURRENT_BYTES + " INTEGER ,")
		.append(JDownloadTaskColumn.TITLE + " TEXT ,")
		.append(JDownloadTaskColumn.DESCRIPTION + " TEXT ,")
		.append(JDownloadTaskColumn.ERROR_MSG + " TEXT ,")
		.append(JDownloadTaskColumn.MD5 + " TEXT ,")
		.append(JDownloadTaskColumn.SPEED + " INTEGER ,")
		.append(JDownloadTaskColumn.TIME_USE + " INTEGER ")
		.append(")")
		.toString();
	}
	
	public static final class JDownloadTaskColumnIndex {
		public static final int ID = 0;
		public static final int URI = 1;
		public static final int DATA = 2;
		public static final int STATUS = 3;
		public static final int NOTIFICATION_CLASS = 4;
		public static final int TOTAL_BYTES = 5;
		public static final int CURRENT_BYTES = 6;
		public static final int TITLE = 7;
		public static final int DESCRIPTION = 8;
		public static final int ERROR_MSG = 9;
		public static final int MD5 =10;        
		public static final int SPEED=11;        
		public static final int TIME_USE =12;
	}
	
	public static final class JDownloadTaskColumn implements BaseColumns{
		//public static final Uri CONTENT_URI = Uri.parse("content://" + GoogleDownloadProvider.AUTHORITY + "/"+DOWNLOAD_TABLE);
		public static final String ID = "_id";
		public static final String URI = "uri";
		public static final String DATA = "_data";
		public static final String STATUS = "status";
		public static final String NOTIFICATION_CLASS = "notificationclass";
		public static final String TOTAL_BYTES = "total_bytes";
		public static final String CURRENT_BYTES = "current_bytes";
		public static final String TITLE = "title";
		public static final String DESCRIPTION = "description";
		public static final String ERROR_MSG = "errorMsg";
		public static final String MD5 = "md5";
		public static final String SPEED = "speed";
		public static final String TIME_USE = "time_use";
	}
	
	private JDownloadDBHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}
	
	public static JDownloadDBHelper getInstance(Context context) {
		if (mJHelper == null) {
			synchronized (JDownloadDBHelper.class) {
				if (mJHelper == null) {
					mJHelper = new JDownloadDBHelper(context);
				}
			}
		}
		return mJHelper;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		CLog.d(TAG, "onCreate");
		db.execSQL(CREATE_DOWNLOAD_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		CLog.d(TAG, "onUpgrade");
	}
	
}