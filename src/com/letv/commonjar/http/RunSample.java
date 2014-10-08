/**
 * =====================================================================
 *
 * @file  JRunSample.java
 * @Module Name   com.joysee.common.data
 * @author benz
 * @OS version  1.0
 * @Product type: JoySee
 * @date   2013-12-11
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
 * benz          2013-12-11           1.0         Check for NULL, 0 h/w
 * =====================================================================
 **/


package com.letv.commonjar.http;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;

class RunSample {
	
	Context ctx;
	
	public void main(String[] args) {
		
		/**
		 * 回调已改为 子线程调用 
		 */
		
		
		/** 简单取JSON */
		HttpHelper.getJson("www.google.com", new HttpJsonCallBack() {
			@Override
			public void onSuccess(byte[] json) {
				
			}
			@Override
			public void onFailure(int errorCode, Throwable e) {
				
			}
		});
		
		
		/** 带参数的请求 */
		RequestParams params = new RequestParams();
		params.put("key1", "value1");
		params.put("key2", "value2");
		HttpHelper.getJson("www.google.com", params, new HttpJsonCallBack() {
			@Override
			public void onSuccess(byte[] json) {
				
			}
			@Override
			public void onFailure(int errorCode, Throwable e) {
				
			}
		});
		
		
		
		/**
		 * 请求JSON，解析并返回 
		 * 
		 * 1.使用 JHttpParserCallBack 接口
		 * 2.重写一个parser ，继承 JBaseParser
		 * 
		 * */
		BaseParser<Object> parserBean=null;
		HttpHelper.getJson("www.google.com", new HttpParserCallBack(parserBean) {
			@Override
			public void onSuccess(Object obj) {
				
			}
			@Override
			public void onFailure(int errorCode, Throwable e) {
				
			}
		});
		
		
		
		/**
		 * 请求图片
		 * */
		HttpHelper.getImage(ctx, "www.google.com.jpg", new FetchBackListener() {
			@Override
			public void fetchSuccess(String key, BitmapDrawable drawable) {
				
			}
		});
		
		
	}
}
