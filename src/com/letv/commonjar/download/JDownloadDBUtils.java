package com.letv.commonjar.download;

import com.letv.commonjar.download.JDownloadDBHelper.JDownloadTaskColumn;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class JDownloadDBUtils {

	private JDownloadDBHelper mDbHelper;

	public JDownloadDBUtils(Context ctx) {
		mDbHelper = JDownloadDBHelper.getInstance(ctx);
	}

	public long insert(ContentValues values) {
		long ret = -1;
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			ret = db.insert(JDownloadDBHelper.DOWNLOAD_TABLE, null, values);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		return ret;
	}

	public long delete(long id) {
		long ret = -1;
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		ret = db.delete(JDownloadDBHelper.DOWNLOAD_TABLE, "_id=?", new String[] { id + "" });
		return ret;
	}

	public long delete(long... ids) {
		long ret = -1;
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		ret = db.delete(JDownloadDBHelper.DOWNLOAD_TABLE, getWhereClauseForIds(ids), getWhereArgsForIds(ids));
		db.close();
		return ret;
	}

	public synchronized long update(long id, ContentValues values) {
		long ret = -1;
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			ret = db.update(JDownloadDBHelper.DOWNLOAD_TABLE, values, "_id=?", new String[] { id + "" });
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		return ret;
	}

	public Cursor query(long id) {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		return db.query(JDownloadDBHelper.DOWNLOAD_TABLE, null, "_id=?", new String[] { id + "" }, null, null, null);
	}

	public Cursor queryAll() {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		return db.query(JDownloadDBHelper.DOWNLOAD_TABLE, null, null, null, null, null, null);
	}

	/**
	 * Get a parameterized SQL WHERE clause to select a bunch of IDs.
	 */
	private static String getWhereClauseForIds(long[] ids) {
		StringBuilder whereClause = new StringBuilder();
		whereClause.append("(");
		for (int i = 0; i < ids.length; i++) {
			if (i > 0) {
				whereClause.append("OR ");
			}
			whereClause.append(JDownloadTaskColumn._ID);
			whereClause.append(" = ? ");
		}
		whereClause.append(")");
		return whereClause.toString();
	}

	/**
	 * Get the selection args for a clause returned by
	 */
	private static String[] getWhereArgsForIds(long[] ids) {
		String[] whereArgs = new String[ids.length];
		for (int i = 0; i < ids.length; i++) {
			whereArgs[i] = Long.toString(ids[i]);
		}
		return whereArgs;
	}
}
