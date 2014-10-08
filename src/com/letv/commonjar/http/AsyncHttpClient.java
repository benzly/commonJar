/**
 * =====================================================================
 *
 * @file AsyHttpClient.java
 * @Module Name com.benz.tools.http
 * @author benz
 * @OS version 1.0
 * @Product type: JoySee
 * @date 2013-11-26
 * @brief This file is the http **** implementation.
 * @This file is responsible by ANDROID TEAM.
 * @Comments: ===================================================================== Revision
 *            History:
 *
 *            Modification Tracking
 *
 *            Author Date OS version Reason ---------- ------------ ------------- ----------- benz
 *            2013-11-26 1.0 Check for NULL, 0 h/w
 *            =====================================================================
 **/
//

package com.letv.commonjar.http;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.SyncBasicHttpContext;

import com.letv.commonjar.CLog;

import android.content.Context;

class AsyncHttpClient {

    public static final String TAG = CLog.makeTag(AsyncHttpClient.class);

    public static final String VERSION = "1.0.0";

    // 并行线程数
    public static final int DEFAULT_MAX_CONNECTIONS = 10;
    // 超时时间
    public static final int DEFAULT_SOCKET_TIMEOUT = 10 * 1000;
    // 重试次数
    public static final int DEFAULT_MAX_RETRIES = 1;
    // 重试时间
    public static final int DEFAULT_RETRY_SLEEP_TIME_MILLIS = 1500;

    public static final int DEFAULT_SOCKET_BUFFER_SIZE = 8192;

    public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
    public static final String ENCODING_GZIP = "gzip";

    private int maxConnections = DEFAULT_MAX_CONNECTIONS;
    private int timeout = DEFAULT_SOCKET_TIMEOUT;

    private boolean isUrlEncodingEnabled = true;

    private ExecutorService mThreadPool;
    private final HttpContext mHttpContext;
    private final DefaultHttpClient mHttpClient;
    private static AsyncHttpClient mAsyncHttpClient;
    private final Map<String, String> mHeaderClientMap;
    private final Map<String, WeakReference<Future<?>>> mUrlRequestMap;
    private final Map<Context, List<WeakReference<Future<?>>>> mContextRequestMap;

    public AsyncHttpClient() {
        this(false, 80, 443);
    }

    private AsyncHttpClient(boolean fixNoHttpResponseException, int httpPort, int httpsPort) {
        this(getDefaultSchemeRegistry(fixNoHttpResponseException, httpPort, httpsPort));
    }

    public static AsyncHttpClient getInstance() {
        if (mAsyncHttpClient == null) {
            synchronized (AsyncHttpClient.class) {
                if (mAsyncHttpClient == null) {
                    mAsyncHttpClient = new AsyncHttpClient();
                }
            }
        }
        return mAsyncHttpClient;
    }

    /**
     * Returns default instance of SchemeRegistry
     */
    private static SchemeRegistry getDefaultSchemeRegistry(boolean fixNoHttpResponseException, int httpPort, int httpsPort) {
        if (fixNoHttpResponseException) {
            CLog.d(TAG, "Beware! Using the fix is insecure, as it doesn't verify SSL certificates.");
        }

        if (httpPort < 1) {
            httpPort = 80;
            CLog.d(TAG, "Invalid HTTP port number specified, defaulting to 80");
        }

        if (httpsPort < 1) {
            httpsPort = 443;
            CLog.d(TAG, "Invalid HTTPS port number specified, defaulting to 443");
        }

        // Fix to SSL flaw in API < ICS
        // See https://code.google.com/p/android/issues/detail?id=13117
        SSLSocketFactory sslSocketFactory;
        if (fixNoHttpResponseException) {
            sslSocketFactory = MySSLSocketFactory.getFixedSocketFactory();
        } else {
            sslSocketFactory = SSLSocketFactory.getSocketFactory();
        }

        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), httpPort));
        schemeRegistry.register(new Scheme("https", sslSocketFactory, httpsPort));

        return schemeRegistry;
    }

    public AsyncHttpClient(SchemeRegistry schemeRegistry) {

        BasicHttpParams httpParams = new BasicHttpParams();

        ConnManagerParams.setTimeout(httpParams, timeout);
        ConnManagerParams.setMaxConnectionsPerRoute(httpParams, new ConnPerRouteBean(maxConnections));
        ConnManagerParams.setMaxTotalConnections(httpParams, DEFAULT_MAX_CONNECTIONS);

        HttpConnectionParams.setSoTimeout(httpParams, timeout);
        HttpConnectionParams.setConnectionTimeout(httpParams, timeout);
        HttpConnectionParams.setTcpNoDelay(httpParams, true);
        HttpConnectionParams.setSocketBufferSize(httpParams, DEFAULT_SOCKET_BUFFER_SIZE);

        HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setUserAgent(httpParams, String.format("android-async-http/%s (http://loopj.com/android-async-http)", VERSION));

        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(httpParams, schemeRegistry);

        mThreadPool = Executors.newFixedThreadPool(DEFAULT_MAX_CONNECTIONS);

        mUrlRequestMap = new HashMap<String, WeakReference<Future<?>>>();
        mContextRequestMap = new WeakHashMap<Context, List<WeakReference<Future<?>>>>();
        mHeaderClientMap = new HashMap<String, String>();

        mHttpContext = new SyncBasicHttpContext(new BasicHttpContext());
        mHttpClient = new DefaultHttpClient(cm, httpParams);
        mHttpClient.addRequestInterceptor(new HttpRequestInterceptor() {
            @Override
            public void process(HttpRequest request, HttpContext context) {
                if (!request.containsHeader(HEADER_ACCEPT_ENCODING)) {
                    request.addHeader(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
                }
                for (String header : mHeaderClientMap.keySet()) {
                    request.addHeader(header, mHeaderClientMap.get(header));
                }
            }
        });

        mHttpClient.addResponseInterceptor(new HttpResponseInterceptor() {
            @Override
            public void process(HttpResponse response, HttpContext context) {
                final HttpEntity entity = response.getEntity();
                if (entity == null) {
                    return;
                }
                final Header encoding = entity.getContentEncoding();
                if (encoding != null) {
                    for (HeaderElement element : encoding.getElements()) {
                        if (element.getName().equalsIgnoreCase(ENCODING_GZIP)) {
                            response.setEntity(new InflatingEntity(entity));
                            break;
                        }
                    }
                }
            }
        });
    }

    public HttpClient getHttpClient() {
        return this.mHttpClient;
    }

    public HttpContext getHttpContext() {
        return this.mHttpContext;
    }

    public void setCookieStore(CookieStore cookieStore) {
        mHttpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }

    public void setThreadPool(ThreadPoolExecutor threadPool) {
        this.mThreadPool = threadPool;
    }

    /**
     * 是否开启重定向
     * 
     * @param enableRedirects
     */
    public void setEnableRedirects(final boolean enableRedirects) {
        mHttpClient.setRedirectHandler(new DefaultRedirectHandler() {
            @Override
            public boolean isRedirectRequested(HttpResponse response, HttpContext context) {
                return enableRedirects;
            }
        });
    }

    public void setUserAgent(String userAgent) {
        HttpProtocolParams.setUserAgent(this.mHttpClient.getParams(), userAgent);
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        if (maxConnections < 1) {
            maxConnections = DEFAULT_MAX_CONNECTIONS;
        }
        this.maxConnections = maxConnections;
        final HttpParams httpParams = this.mHttpClient.getParams();
        ConnManagerParams.setMaxConnectionsPerRoute(httpParams, new ConnPerRouteBean(this.maxConnections));
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        if (timeout < 1000) {
            timeout = DEFAULT_SOCKET_TIMEOUT;
        }
        this.timeout = timeout;
    }

    public void setProxy(String hostname, int port) {
        final HttpHost proxy = new HttpHost(hostname, port);
        final HttpParams httpParams = this.mHttpClient.getParams();
        httpParams.setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
    }

    public void setProxy(String hostname, int port, String username, String password) {
        AuthScope authScope = new AuthScope(hostname, port);
        UsernamePasswordCredentials uCredentials = new UsernamePasswordCredentials(username, password);
        mHttpClient.getCredentialsProvider().setCredentials(authScope, uCredentials);
        final HttpHost proxy = new HttpHost(hostname, port);
        final HttpParams httpParams = this.mHttpClient.getParams();
        httpParams.setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
    }

    /**
     * Sets the SSLSocketFactory to user when making requests. By default, a new, default
     * SSLSocketFactory is used.
     */
    public void setSSLSocketFactory(SSLSocketFactory sslSocketFactory) {
        this.mHttpClient.getConnectionManager().getSchemeRegistry().register(new Scheme("https", sslSocketFactory, 443));
    }

    /**
     * 设置重试次数和超时时的重试时间
     */
    public void setMaxRetriesAndTimeout(int retries, int retrySleep) {
        this.mHttpClient.setHttpRequestRetryHandler(new RetryHandler(retries, retrySleep));
    }

    /**
     * Sets headers that will be added to all requests this client makes (before sending).
     */
    public void addHeader(String header, String value) {
        mHeaderClientMap.put(header, value);
    }

    /**
     * Remove header from all requests this client makes (before sending).
     */
    public void removeHeader(String header) {
        mHeaderClientMap.remove(header);
    }

    /**
     * Sets basic authentication for the request. Uses AuthScope.ANY. This is the same as
     * setBasicAuth('username','password',AuthScope.ANY)
     */
    public void setBasicAuth(String username, String password) {
        AuthScope scope = AuthScope.ANY;
        setBasicAuth(username, password, scope);
    }

    /**
     * Sets basic authentication for the request. You should pass in your AuthScope for security. It
     * should be like this setBasicAuth("username","password", new
     * AuthScope("host",port,AuthScope.ANY_REALM))
     */
    public void setBasicAuth(String username, String password, AuthScope scope) {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
        this.mHttpClient.getCredentialsProvider().setCredentials(scope, credentials);
    }

    /**
     * Removes set basic auth credentials
     */
    public void clearBasicAuth() {
        this.mHttpClient.getCredentialsProvider().clear();
    }

    /**
     * Cancels any pending (or potentially active) requests associated with the passed Context. This
     * will only affect requests which were created with a non-null android Context. This method is
     * intended to be used in the onDestroy method of your android activities to destroy all
     * requests which are no longer required
     */
    public void cancelRequests(Context context, boolean mayInterruptIfRunning) {
        synchronized (mContextRequestMap) {
            List<WeakReference<Future<?>>> requestList = mContextRequestMap.get(context);
            if (requestList != null) {
                for (WeakReference<Future<?>> requestRef : requestList) {
                    if (requestRef != null) {
                        Future<?> request = requestRef.get();
                        if (request != null) {
                            request.cancel(mayInterruptIfRunning);
                        }
                        mContextRequestMap.remove(context);
                    }
                }
            }
        }
    }

    public void cancelRequest(String url, boolean mayInterruptIfRunning) {
        synchronized (mUrlRequestMap) {
            WeakReference<Future<?>> requestRef = mUrlRequestMap.get(url);
            if (requestRef != null) {
                Future<?> request = requestRef.get();
                if (request != null) {
                    request.cancel(mayInterruptIfRunning);
                }
                mUrlRequestMap.remove(url);
            }
        }
    }

    public void cancelRequest(ArrayList<String> urls, boolean mayInterruptIfRunning) {
        if (urls == null) {
            return;
        }
        synchronized (mUrlRequestMap) {
            int size = urls.size();
            long begin = CLog.methodBegin(TAG);
            for (int i = 0; i < size; i++) {
                String url = urls.get(i);
                WeakReference<Future<?>> requestRef = mUrlRequestMap.get(url);
                if (requestRef != null) {
                    Future<?> request = requestRef.get();
                    if (request != null) {
                        request.cancel(mayInterruptIfRunning);
                    }
                    mUrlRequestMap.remove(url);
                }
            }
            CLog.methodEnd(TAG, begin, size + " count");
        }
    }

    public void cancelAllRequest(boolean mayInterruptIfRunning) {
        synchronized (mUrlRequestMap) {
            Iterator<String> it = mUrlRequestMap.keySet().iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                WeakReference<Future<?>> requestRef = mUrlRequestMap.get(key);
                if (requestRef != null) {
                    Future<?> request = requestRef.get();
                    if (request != null) {
                        request.cancel(mayInterruptIfRunning);
                    }
                }
            }
            mUrlRequestMap.clear();
        }
    }

    /**
     * Perform a HTTP HEAD request, without any parameters.
     */
    @SuppressWarnings("unused")
    private RequestReceipt head(String url, ResponseInterface responseInterface) {
        return head(null, url, null, responseInterface);
    }

    /**
     * Perform a HTTP HEAD request with parameters.
     */
    @SuppressWarnings("unused")
    private RequestReceipt head(String url, RequestParams params, ResponseInterface responseHandler) {
        return head(null, url, params, responseHandler);
    }

    /**
     * Perform a HTTP HEAD request without any parameters and track the Android Context which
     * initiated the request.
     */
    @SuppressWarnings("unused")
    private RequestReceipt head(Context context, String url, ResponseInterface responseHandler) {
        return head(context, url, null, responseHandler);
    }

    /**
     * Perform a HTTP HEAD request and track the Android Context which initiated the request.
     */
    private RequestReceipt head(Context context, String url, RequestParams params, ResponseInterface responseInterface) {
        HttpHead head = new HttpHead(getUrlWithQueryString(isUrlEncodingEnabled, url, params));
        return sendRequest(mHttpClient, mHttpContext, head, null, responseInterface, context);
    }

    /**
     * Perform a HTTP HEAD request and track the Android Context which initiated the request with
     * customized headers
     */
    @SuppressWarnings("unused")
    private RequestReceipt head(Context context, String url, Header[] headers, RequestParams params, ResponseInterface responseHandler) {
        HttpUriRequest request = new HttpHead(getUrlWithQueryString(isUrlEncodingEnabled, url, params));
        if (headers != null) {
            request.setHeaders(headers);
        }
        return sendRequest(mHttpClient, mHttpContext, request, null, responseHandler, context);
    }

    /**
     * @param context
     * @param url
     * @param params
     * @param responseHandler
     * @return
     */
    public RequestReceipt get(Context context, String url, RequestParams params, ResponseInterface responseHandler) {
        HttpGet httpGet = new HttpGet(getUrlWithQueryString(isUrlEncodingEnabled, url, params));
        return sendRequest(mHttpClient, mHttpContext, httpGet, null, responseHandler, context);
    }

    /**
     * @param context
     * @param url
     * @param headers
     * @param params
     * @param responseHandler
     * @return
     */
    public RequestReceipt get(Context context, String url, Header[] headers, RequestParams params, ResponseInterface responseHandler) {
        HttpUriRequest request = new HttpGet(getUrlWithQueryString(isUrlEncodingEnabled, url, params));
        if (headers != null) {
            request.setHeaders(headers);
        }
        return sendRequest(mHttpClient, mHttpContext, request, null, responseHandler, context);
    }

    /**
     * @param context
     * @param url
     * @param entity
     * @param contentType
     * @param responseHandler
     * @return
     */
    public RequestReceipt post(Context context, String url, HttpEntity entity, String contentType, ResponseInterface responseHandler) {
        HttpPost httpPost = new HttpPost(url);
        return sendRequest(mHttpClient, mHttpContext, addEntityToRequestBase(httpPost, entity), contentType, responseHandler, context);
    }

    /**
     * @param context
     * @param url
     * @param headers
     * @param params
     * @param contentType
     * @param r
     * @return
     */
    public RequestReceipt post(Context context, String url, Header[] headers, RequestParams params, String contentType,
            ResponseInterface r) {
        HttpEntityEnclosingRequestBase request = new HttpPost(url);
        if (params != null) {
            request.setEntity(paramsToEntity(params, r));
        }
        if (headers != null) {
            request.setHeaders(headers);
        }
        return sendRequest(mHttpClient, mHttpContext, request, contentType, r, context);
    }

    /**
     * @param context
     * @param url
     * @param headers
     * @param entity
     * @param contentType
     * @param r
     * @return
     */
    public RequestReceipt post(Context context, String url, Header[] headers, HttpEntity entity, String contentType, ResponseInterface r) {
        HttpEntityEnclosingRequestBase request = addEntityToRequestBase(new HttpPost(url), entity);
        if (headers != null) {
            request.setHeaders(headers);
        }
        return sendRequest(mHttpClient, mHttpContext, request, contentType, r, context);
    }

    /**
     * @param url
     * @param responseHandler
     * @return
     */
    public RequestReceipt put(String url, ResponseInterface responseHandler) {
        return put(null, url, null, responseHandler);
    }

    /**
     * @param url
     * @param params
     * @param responseHandler
     * @return
     */
    public RequestReceipt put(String url, RequestParams params, ResponseInterface responseHandler) {
        return put(null, url, params, responseHandler);
    }

    /**
     * @param context
     * @param url
     * @param params
     * @param responseHandler
     * @return
     */
    public RequestReceipt put(Context context, String url, RequestParams params, ResponseInterface responseHandler) {
        return put(context, url, paramsToEntity(params, responseHandler), null, responseHandler);
    }

    /**
     * Perform a HTTP PUT request and track the Android Context which initiated the request. And set
     * one-time headers for the request
     */
    public RequestReceipt put(Context context, String url, HttpEntity entity, String contentType, ResponseInterface responseHandler) {
        HttpPut httpPut = new HttpPut(url);
        return sendRequest(mHttpClient, mHttpContext, addEntityToRequestBase(httpPut, entity), contentType, responseHandler, context);
    }

    /**
     * Perform a HTTP PUT request and track the Android Context which initiated the request. And set
     * one-time headers for the request
     */
    public RequestReceipt put(Context context, String url, Header[] headers, HttpEntity entity, String contentType, ResponseInterface r) {
        HttpEntityEnclosingRequestBase request = addEntityToRequestBase(new HttpPut(url), entity);
        if (headers != null) {
            request.setHeaders(headers);
        }
        return sendRequest(mHttpClient, mHttpContext, request, contentType, r, context);
    }

    /**
     * Perform a HTTP DELETE request.
     */
    public RequestReceipt delete(String url, ResponseInterface responseHandler) {
        return delete(null, url, responseHandler);
    }

    /**
     * Perform a HTTP DELETE request.
     */
    public RequestReceipt delete(Context context, String url, ResponseInterface responseHandler) {
        final HttpDelete delete = new HttpDelete(url);
        return sendRequest(mHttpClient, mHttpContext, delete, null, responseHandler, context);
    }

    /**
     * Perform a HTTP DELETE request.
     */
    public RequestReceipt delete(Context context, String url, Header[] headers, ResponseInterface responseHandler) {
        final HttpDelete delete = new HttpDelete(url);
        if (headers != null) {
            delete.setHeaders(headers);
        }
        return sendRequest(mHttpClient, mHttpContext, delete, null, responseHandler, context);
    }

    /**
     * Perform a HTTP DELETE request.
     */
    public RequestReceipt delete(Context context, String url, Header[] headers, RequestParams params, ResponseInterface r) {
        HttpDelete httpDelete = new HttpDelete(getUrlWithQueryString(isUrlEncodingEnabled, url, params));
        if (headers != null) {
            httpDelete.setHeaders(headers);
        }
        return sendRequest(mHttpClient, mHttpContext, httpDelete, null, r, context);
    }

    /**
     * Create a request
     * 
     * @param client
     * @param mHttpContext
     * @param uriRequest
     * @param contentType
     * @param responseInterface
     * @param context
     * @return
     */
    protected RequestReceipt sendRequest(DefaultHttpClient client, HttpContext httpContext, HttpUriRequest uriRequest, //
            String contentType, ResponseInterface responseInterface, Context context) {

        if (contentType != null) {
            uriRequest.setHeader("Content-Type", contentType);
        }

        responseInterface.setRequestHeaders(uriRequest.getAllHeaders());
        responseInterface.setRequestURI(uriRequest.getURI());

        Future<?> request = mThreadPool.submit(new AsyncHttpRequest(client, httpContext, timeout, uriRequest, responseInterface));

        // TODO: Remove dead Weakrefs from requestLists?
        if (context != null) {
            List<WeakReference<Future<?>>> requestList = mContextRequestMap.get(context);
            if (requestList == null) {
                requestList = new LinkedList<WeakReference<Future<?>>>();
                mContextRequestMap.put(context, requestList);
            }
            requestList.add(new WeakReference<Future<?>>(request));
        }

        String url = uriRequest.getURI().toASCIIString();
        CLog.d(TAG, "sendRequest = " + url);
        if (url != null) {
            mUrlRequestMap.put(url, new WeakReference<Future<?>>(request));
        }

        return new RequestReceipt(request);
    }

    /**
     * Sets state of URL encoding feature, see bug #227, this method allows you to turn off and on
     * this auto-magic feature on-demand.
     */
    public void setURLEncodingEnabled(boolean enabled) {
        this.isUrlEncodingEnabled = enabled;
    }

    /**
     * Will encode url, if not disabled, and adds params on the end of it
     */
    public static String getUrlWithQueryString(boolean shouldEncodeUrl, String url, RequestParams params) {
        if (shouldEncodeUrl) {
            url = url.replace(" ", "%20");
        }
        if (params != null) {
            String paramString = params.getParamString();
            if (!url.contains("?")) {
                url += "?" + paramString;
            } else {
                url += "&" + paramString;
            }
        }
        CLog.d(TAG, "add params to url=" + url);
        return url;
    }

    /**
     * Returns HttpEntity containing data from RequestParams included with request declaration.
     * Allows also passing progress from upload via provided ResponseHandler
     */
    private HttpEntity paramsToEntity(RequestParams params, ResponseInterface responseHandler) {
        HttpEntity entity = null;
        try {
            if (params != null) {
                entity = params.getEntity(responseHandler);
            }
        } catch (Throwable t) {
            throw new IllegalArgumentException("JRequestParams  Error=" + t.getMessage());
        }
        return entity;
    }

    public boolean isUrlEncodingEnabled() {
        return isUrlEncodingEnabled;
    }

    /**
     * Applicable only to HttpRequest methods extending HttpEntityEnclosingRequestBase, which is for
     * example not DELETE
     */
    private HttpEntityEnclosingRequestBase addEntityToRequestBase(HttpEntityEnclosingRequestBase requestBase, HttpEntity entity) {
        if (entity != null) {
            requestBase.setEntity(entity);
        }
        return requestBase;
    }

    /**
     * Enclosing entity to hold stream of gzip decoded data for accessing HttpEntity contents
     */
    private static class InflatingEntity extends HttpEntityWrapper {
        public InflatingEntity(HttpEntity wrapped) {
            super(wrapped);
        }

        @Override
        public InputStream getContent() throws IOException {
            return new GZIPInputStream(wrappedEntity.getContent());
        }

        @Override
        public long getContentLength() {
            return -1;
        }
    }

}
