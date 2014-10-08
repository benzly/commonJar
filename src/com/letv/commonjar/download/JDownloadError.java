package com.letv.commonjar.download;

public class JDownloadError {

	public static class NetTimeOut extends Exception {

		private static final long serialVersionUID = -6167870496227281633L;

		public NetTimeOut(String msg) {
			super(msg);
		}
	}

	public static class PrepareException extends Exception {

		private static final long serialVersionUID = 8228669551569558025L;

		public PrepareException(String msg) {
			super(msg);
		}
	}
	
}
