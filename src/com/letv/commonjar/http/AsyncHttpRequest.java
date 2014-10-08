/**
 * =====================================================================
 *
 * @file  JAsyncHttpRequest.java
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

import com.letv.commonjar.CLog;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;



import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

class AsyncHttpRequest implements Runnable {
    private static final String TAG = CLog.makeTag(AsyncHttpRequest.class);
    private final AbstractHttpClient client;
    private final HttpContext context;
    private final HttpUriRequest request;
    private final ResponseInterface responseInterface;
    private int executionCount;
    private int timeout;
    private int errorCode = -1;

    public AsyncHttpRequest(AbstractHttpClient c, HttpContext ctx, int timeout, HttpUriRequest hr, ResponseInterface rf) {
        this.client = c;
        this.context = ctx;
        this.timeout = timeout;
        this.request = hr;
        this.responseInterface = rf;
    }

    @Override
    public void run() {
        if (responseInterface != null) {
            responseInterface.onStart();
        }
        try {
            makeRequestWithRetries();
        } catch (Exception e) {
            if (responseInterface != null) {
                responseInterface.handlerError(errorCode, e);
            }
        }
        if (responseInterface != null) {
            responseInterface.onFinish();
        }
    }

    private void makeRequestWithRetries() throws Exception {
        boolean retry = true;
        HttpRequestRetryHandler retryHandler = null;
        try {
            retryHandler = client.getHttpRequestRetryHandler();
        } catch (Exception e) {
            CLog.e(TAG, "client.getHttpRequestRetryHandler  e=" + e);
            retryHandler = null;
        }

        IOException cause = null;
        while (retry) {
            try {
                makeRequest();
                break;
            } catch (UnknownHostException e) {
                cause = new IOException("UnknownHostException exception: " + e.getMessage());
                errorCode = RequsetError.ERROR_CODE_UNKNOWNHOST;
                retry = retryHandler != null && (executionCount > 0) && retryHandler.retryRequest(cause, ++executionCount, context);
            } catch (IOException e) {
                cause = e;
                errorCode = RequsetError.ERROR_CODE_IO;
                retry = retryHandler != null && retryHandler.retryRequest(cause, ++executionCount, context);
            } catch (Exception e) {
                cause = new IOException(e.getMessage());
                retry = retryHandler != null && retryHandler.retryRequest(cause, ++executionCount, context);
            }
            if (retry && (responseInterface != null)) {
                responseInterface.onRetryCount(executionCount);
            }
        }
        if (cause != null) {
            throw cause;
        }
    }

    private void makeRequest() throws Exception {
        if (!Thread.currentThread().isInterrupted()) {
            if (request.getURI().getScheme() == null) {
                throw new MalformedURLException(CLog.makeTag(AsyncHttpRequest.class) + "--> No valid URI scheme was provided");
            }
            setTimeout(timeout);
            try {
                HttpResponse response = client.execute(request, context);
                if (!Thread.currentThread().isInterrupted()) {
                    if (responseInterface != null) {
                        responseInterface.handlerResponse(response);
                    }
                }
            } catch (SocketTimeoutException e) {
                errorCode = RequsetError.ERROR_CODE_TIME_OUT;
                throw e;
            } catch (Exception e) {
                errorCode = RequsetError.ERROR_CODE_UNKOWN;
                CLog.e(TAG, "client.execute exception e=" + e);
                throw e;
            }
        }
    }

    private void setTimeout(int timeout) {
        if (timeout < 1000) {
            timeout = AsyncHttpClient.DEFAULT_SOCKET_TIMEOUT;
        }
        final HttpParams httpParams = client.getParams();
        ConnManagerParams.setTimeout(httpParams, this.timeout);
        HttpConnectionParams.setSoTimeout(httpParams, this.timeout);
        HttpConnectionParams.setConnectionTimeout(httpParams, this.timeout);
    }
}
