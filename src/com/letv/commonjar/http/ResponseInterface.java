/**
 * =====================================================================
 *
 * @file  JResponseInterface.java
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

import java.io.IOException;
import java.net.URI;
import org.apache.http.Header;
import org.apache.http.HttpResponse;

interface ResponseInterface {
	
	void handlerResponse(HttpResponse response) throws IOException;
	void handlerError(Integer code, Throwable e);
	
    void onStart();
    void onProgress(int bytesWritten, int bytesTotal);
    void onFinish();
    void onRetryCount(int retryNo);
    
    void setRequestURI(URI requestURI);
    void setRequestHeaders(Header[] requestHeaders);
    void setUseSynchronousMode(boolean useSynchronousMode);

    URI getRequestURI();
    Header[] getRequestHeaders();
    boolean getUseSynchronousMode();
}
