/**
 * =====================================================================
 *
 * @file  JNet.java
 * @Module Name   com.joysee.common.utils
 * @author benz
 * @OS version  1.0
 * @Product type: JoySee
 * @date   2013-12-24
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
 * benz          2013-12-24           1.0         Check for NULL, 0 h/w
 * =====================================================================
 **/


package com.letv.commonjar.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class JNet {

	public static boolean isConnected(Context ctx) {
		boolean ret = false;
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) {
        	ret =  activeNetwork.isConnectedOrConnecting();
        }
        return ret;
    }

}
