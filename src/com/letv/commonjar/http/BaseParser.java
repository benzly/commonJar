/**
 * =====================================================================
 *
 * @file  BaseParser.java
 * @Module Name   com.joysee.common.data
 * @author benz
 * @OS version  1.0
 * @Product type: JoySee
 * @date   2013-12-10
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
 * benz          2013-12-10           1.0         Check for NULL, 0 h/w
 * =====================================================================
 **/


package com.letv.commonjar.http;

import org.json.JSONException;


public abstract class BaseParser<T> {
	
	public abstract T parseJSON(String jsonString) throws JSONException;
	
	public abstract String checkResponse(String jsonString) throws JSONException ;
}
