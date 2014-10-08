package com.letv.commonjar.download;

public class JDownloadInfo {
	public long mId;
	public String mUri;
	public String mData;
	public int mStatus;
	public String mClass;
	public long mTotalBytes;
	public long mCurrentBytes;
	public int mUid;
	public String mTitle;
	public String mDescription;
	public int mProgress = 0;
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("mId="+mId + " \n");
		sb.append("url= "+mUri + " \n");
		sb.append("data="+mData + " \n");
		sb.append("title="+mTitle + " \n");
		return sb.toString();
	}
}
