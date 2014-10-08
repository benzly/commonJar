/**
 * =====================================================================
 *
 * @file  JFetchBackListener.java
 * @Module Name   com.joysee.common.data
 * @author benz
 * @OS version  1.0
 * @Product type: JoySee
 * @date   2013-12-6
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
 * benz          2013-12-6           1.0         Check for NULL, 0 h/w
 * =====================================================================
 **/
//


package com.letv.commonjar.http;

import android.graphics.drawable.BitmapDrawable;

public interface FetchBackListener {
	public void fetchSuccess(String key, BitmapDrawable drawable);
}
