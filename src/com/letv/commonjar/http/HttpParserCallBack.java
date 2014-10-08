/**
 * =====================================================================
 *
 * @file JHttpBeanCallBack.java
 * @Module Name com.joysee.common.data
 * @author benz
 * @OS version 1.0
 * @Product type: JoySee
 * @date 2013-12-10
 * @brief This file is the http **** implementation.
 * @This file is responsible by ANDROID TEAM.
 * @Comments: ===================================================================== Revision
 *            History:
 *
 *            Modification Tracking
 *
 *            Author Date OS version Reason ---------- ------------ ------------- ----------- benz
 *            2013-12-10 1.0 Check for NULL, 0 h/w
 *            =====================================================================
 **/

package com.letv.commonjar.http;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONException;

import com.letv.commonjar.CLog;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public abstract class HttpParserCallBack implements ResponseInterface {

    private static final String TAG = CLog.makeTag(HttpParserCallBack.class);

    protected static final int SUCCESS_MESSAGE = 0;
    protected static final int FAILURE_MESSAGE = 1;
    protected static final int START_MESSAGE = 2;
    protected static final int FINISH_MESSAGE = 3;
    protected static final int PROGRESS_MESSAGE = 4;
    protected static final int RETRY_MESSAGE = 5;

    protected static final int BUFFER_SIZE = 4096;

    public static final String DEFAULT_CHARSET = "UTF-8";
    private String responseCharset = DEFAULT_CHARSET;
    private Boolean useSynchronousMode = false;

    private URI requestURI = null;
    private Header[] requestHeaders = null;

    BaseParser<?> parser;

    public abstract void onSuccess(Object obj);

    public abstract void onFailure(int errorCode, Throwable e);

    public HttpParserCallBack(BaseParser<?> parser) {
        this.parser = parser;
    }

    @Override
    public URI getRequestURI() {
        return this.requestURI;
    }

    @Override
    public Header[] getRequestHeaders() {
        return this.requestHeaders;
    }

    @Override
    public void setRequestURI(URI requestURI) {
        this.requestURI = requestURI;
    }

    @Override
    public void setRequestHeaders(Header[] requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    @Override
    public boolean getUseSynchronousMode() {
        return useSynchronousMode;
    }

    @Override
    public void setUseSynchronousMode(boolean value) {
        useSynchronousMode = value;
    }

    public void setCharset(final String charset) {
        this.responseCharset = charset;
    }

    public String getCharset() {
        return this.responseCharset == null ? DEFAULT_CHARSET : this.responseCharset;
    }

    public void onProgress(int bytesWritten, int totalSize) {
        if (totalSize != 0) {
            CLog.d(TAG, String.format("J Http Progress %d from %d (%d)", bytesWritten, totalSize, bytesWritten * 100 / totalSize));
        }
    }

    /**
     * Fired when a retry occurs, override to handle in your own code
     * 
     * @param retryNo number of retry
     */
    public void onRetryCount(int retryNo) {
        CLog.d(TAG, String.format("Request retry no. %d", retryNo));
    }

    public void onStart() {

    }

    public void onFinish() {

    }

    /**
     * handler response data
     * 
     * @throws IOException
     */
    @Override
    public void handlerResponse(HttpResponse response) throws IOException {
        if (!Thread.currentThread().isInterrupted()) {
            StatusLine status = response.getStatusLine();
            byte[] responseBody;
            responseBody = getResponseData(response.getEntity());
            if (!Thread.currentThread().isInterrupted()) {
                if (status.getStatusCode() >= 300) {
                    onFailure(status.getStatusCode(), new HttpResponseException(status.getStatusCode(), status.getReasonPhrase()));
                } else {
                    String json = new String(responseBody);
                    Object obj = null;
                    JSONException error = null;
                    try {
                        obj = parser.parseJSON(json);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        obj = null;
                        error = e;
                    }
                    if (obj != null) {
                        onSuccess(obj);
                    } else {
                        if (error == null) {
                            error = new JSONException("parseJSON return null obj");
                        }
                        onFailure(status.getStatusCode(), error);
                    }
                }
            }
        }
    }

    @Override
    public void handlerError(Integer code, Throwable e) {
        onFailure(code, e);
    }

    /**
     * Returns byte array of response HttpEntity contents
     * 
     * @param entity
     * @return
     * @throws IOException
     */
    byte[] getResponseData(HttpEntity entity) throws IOException {
        byte[] responseBody = null;
        if (entity != null) {
            InputStream instream = entity.getContent();
            if (instream != null) {
                long contentLength = entity.getContentLength();
                if (contentLength > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException("HTTP entity too large to be buffered in memory");
                }
                if (contentLength < 0) {
                    contentLength = BUFFER_SIZE;
                }
                try {
                    ByteArrayBuffer buffer = new ByteArrayBuffer((int) contentLength);
                    try {
                        byte[] tmp = new byte[BUFFER_SIZE];
                        int l, count = 0;
                        while ((l = instream.read(tmp)) != -1 && !Thread.currentThread().isInterrupted()) {
                            count += l;
                            buffer.append(tmp, 0, l);
                            onProgress(count, (int) contentLength);
                        }
                    } finally {
                        instream.close();
                    }
                    responseBody = buffer.toByteArray();
                } catch (OutOfMemoryError e) {
                    System.gc();
                    throw new IOException("File too large to fit into available memory");
                }
            }
        }
        return responseBody;
    }
}
