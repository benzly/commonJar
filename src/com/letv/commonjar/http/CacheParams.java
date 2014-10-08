/**
 * =====================================================================
 *
 * @file  JCacheParams.java
 * @Module Name   com.joysee.common.data
 * @author benz
 * @OS version  1.0
 * @Product type: JoySee
 * @date   2014-1-7
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
 * benz          2014-1-7           1.0         Check for NULL, 0 h/w
 * =====================================================================
 **/


package com.letv.commonjar.http;

import java.io.File;

import com.letv.commonjar.CLog;

import android.content.Context;
import android.os.Environment;

public class CacheParams {

	private String diskCacheDirName;
	/** byte */
	private int memorySize;
	/** byte */
	private int diskSize;
	
	private boolean useMemoryCache = true;
	
	private boolean useDiskCache = true;
	
	private int effectiveTime;
	/** 1~100 */
	private int jpgSaveQuality = 100;
	
	private int defaultLoadingResId;
	
	
	public CacheParams(Context ctx) {
		initDisCacheDir(ctx);
	}
	
	public String getDiskCacheDirName() {
		return diskCacheDirName;
	}
	
	private void initDisCacheDir(Context c) {
	    String dir;
	    if(diskCacheDirName == null || "".equals(diskCacheDirName)){
	        diskCacheDirName = "JDiskCache";
	    }
	    if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) && !Environment.isExternalStorageRemovable()){
	        dir = c.getExternalCacheDir().getPath();
	    }else{
	        dir = c.getCacheDir().getPath();
	    }
	    diskCacheDirName = dir + File.separator + diskCacheDirName;
	    CLog.d("JCacheParams", "DiskCacheDir = "+dir);
	}
	
	public File getDisCacheDirFile(){
		return new File(diskCacheDirName);
	}

	/** LruCache need k byte
	 * @return memorySize
	 */
	public int getMemorySize() {
		if(memorySize == 0){
			memorySize = 1024 * 1024 * 10;
		}
		return memorySize / 1024;
	}

	/**
	 * DiskLruCache need byte
	 * @return
	 */
	public int getDiskSize() {
		if(diskSize == 0){
			diskSize = 1024 * 1024 * 20;
		}
		return diskSize;
	}

	public boolean isUseMemoryCache() {
		return useMemoryCache;
	}

	public boolean isUseDiskCache() {
		return useDiskCache;
	}

	public int getEffectiveTime() {
		return effectiveTime;
	}

	public int getJpgSaveQuality() {
		return jpgSaveQuality;
	}

	public int getDefaultLoadingResId() {
		return defaultLoadingResId;
	}

	public void setDiskCacheDirName(String diskCacheDirName) {
		this.diskCacheDirName = diskCacheDirName;
	}

	/**
	 * @param memorySize  byte
	 */
	public void setMemorySize(int memorySize) {
		if(memorySize >= Runtime.getRuntime().maxMemory()){
			throw new IllegalArgumentException("memory overflow");
		}
		this.memorySize = memorySize;
	}

	/**
	 * @param diskSize  byte
	 */
	public void setDiskSize(int diskSize) {
		this.diskSize = diskSize;
	}

	public void setUseMemoryCache(boolean useMemoryCache) {
		this.useMemoryCache = useMemoryCache;
	}

	public void setUseDiskCache(boolean useDiskCache) {
		this.useDiskCache = useDiskCache;
	}

	public void setEffectiveTime(int effectiveTime) {
		this.effectiveTime = effectiveTime;
	}

	public void setJpgSaveQuality(int jpgSaveQuality) {
		if(jpgSaveQuality<=0 || jpgSaveQuality>100){
			throw new IllegalArgumentException("JpgSaveQuality must be between 1 and 100");
		}
		this.jpgSaveQuality = jpgSaveQuality;
	}

	public void setDefaultLoadingResId(int defaultLoadingResId) {
		this.defaultLoadingResId = defaultLoadingResId;
	}
	
}
