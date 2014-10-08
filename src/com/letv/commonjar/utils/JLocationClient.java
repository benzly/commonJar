/**
 * =====================================================================
 *
 * @file JLocationClient.java
 * @Module Name com.joysee.common.utils
 * @author YueLiang
 * @OS version 1.0
 * @Product type: JoySee
 * @date 2013年10月29日
 * @brief This file is the http **** implementation.
 * @This file is responsible by ANDROID TEAM.
 * @Comments: ===================================================================== Revision
 *            History:
 *
 *            Modification Tracking
 *
 *            Author Date OS version Reason ---------- ------------ ------------- -----------
 *            YueLiang 2013年10月29日 1.0 Check for NULL, 0 h/w
 *            =====================================================================
 **/

package com.letv.commonjar.utils;

import android.content.Context;
import android.os.Handler;

import com.letv.commonjar.CLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;

public class JLocationClient {
    public interface Callback {
        void onDataReady(JLocationClient location);

        void onGetError();
    }

    private static final String TAG = CLog.makeTag(JLocationClient.class);
    private static String entry = "http://api.map.baidu.com/location/ip";
    private static String host = "api.map.baidu.com";

    private static URL url;

    private String ak = "tzUhvMTX9hTzLnKMv7BKmdby";

    public static String getCounty() {
        return mCounty;
    }

    public static String getCity() {
        return mCity;
    }

    public static String getCityCode() {
        return mCityCode;
    }

    public static String getProvince() {
        return mProvince;
    }

    public static boolean isInitialized() {
        return mIsInitialized;
    }

    private Proxy proxy = null;
    private Context mContext;
    private Callback mCallback;

    private static String mCounty;

    private static String mCity;

    private static String mCityCode;

    private static String mProvince;

    private static boolean mIsInitialized;

    public JLocationClient(Context context, Callback callback) {
        if (context == null || callback == null) {
            throw new IllegalArgumentException("context or callback is null.");
        }
        mContext = context;
        mCallback = callback;
        try {
            url = new URL(entry);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void setAk(String ak) {
        this.ak = ak;
    }

    private boolean checkProxy() {
        @SuppressWarnings("deprecation")
        String host = android.net.Proxy.getDefaultHost();
        @SuppressWarnings("deprecation")
        int port = android.net.Proxy.getDefaultPort();
        if (host == null || port == -1) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private Proxy getProxy() {
        if (proxy == null) {
            /* 获取系统代理端口 */
            String host = android.net.Proxy.getDefaultHost();
            int port = android.net.Proxy.getDefaultPort();
            CLog.d(TAG, " host : " + host + "     |||   port " + port);
            SocketAddress sa = new InetSocketAddress(host, port);
            proxy = new Proxy(java.net.Proxy.Type.HTTP, sa);
        }
        return proxy;
    }

    private boolean initDataInternal() {
        HttpURLConnection conn = null;
        InputStream input = null;
        OutputStream output = null;
        boolean ret = false;
        try {
            if (!mIsInitialized) {
                if (ak != null) {
                    String str = url.toString();
                    str += "?ak=" + ak;
                    url = new URL(str);
                }
                CLog.d(TAG, "url = " + url);
                if (checkProxy()) {
                    conn = (HttpURLConnection) url.openConnection(getProxy());
                } else {
                    conn = (HttpURLConnection) url.openConnection();
                }
                conn.setUseCaches(false);
                conn.setConnectTimeout(1000 * 6);
                conn.setReadTimeout(1000 * 6);

                conn.setDoInput(true);
                conn.setRequestProperty("Host", host);
                conn.setRequestProperty("User-Agent", JLocationClient.class.getSimpleName());
                conn.setRequestMethod("GET");

                conn.connect();

                if (isRunning()) {
                    input = conn.getInputStream();

                    InputStreamReader in = new InputStreamReader(input);
                    StringBuilder sb = new StringBuilder();
                    char buf[] = new char[4096];
                    int cnt;
                    String str;

                    while ((cnt = in.read(buf, 0, buf.length)) != -1 && isRunning()) {
                        sb.append(buf, 0, cnt);
                    }

                    str = sb.toString();

                    if (isRunning()) {
                        CLog.d(TAG, ">>>>>>>>>>JSON : " + conn.getResponseCode());
                        CLog.d(TAG, ">>>>>>>>>>JSON : " + str);
                        // CLog.d(TAG, ">>>>>>>>>>JSON : " + JUnicode.decode(str));
                    }
                    if (parseLocation(str)) {
                        mIsInitialized = true;
                        if (mCallback != null) {
                            new Handler(mContext.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    mCallback.onDataReady(JLocationClient.this);
                                }
                            });
                        }
                    }
                }
            } else {
                CLog.d(TAG, "JLocation has already initialized..");
                if (mCallback != null) {
                    new Handler(mContext.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onDataReady(JLocationClient.this);
                        }
                    });
                }
            }

            ret = true;
        } catch (Exception e) {
            CLog.d(TAG, "http request failed! " + e);
            if (mCallback != null) {
                new Handler(mContext.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onGetError();
                    }
                });
            }
        } finally {
            try {
                if (output != null) {
                    output.close();
                }
                if (input != null) {
                    input.close();
                }
                if (conn != null) {
                    conn.disconnect();
                }
            } catch (Exception e) {}
        }

        return ret;
    }

    public boolean initDataSync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                initDataInternal();
            }
        }, JLocationClient.class.getSimpleName()).start();
        return true;
    }

    private boolean isRunning() {
        return true;
    }

    boolean parseLocation(String s) throws JSONException {
        boolean ret = false;
        JSONObject root = new JSONObject(s);
        JSONObject content = (JSONObject) root.get("content");
        CLog.d(TAG, "content = " + content);
        JSONObject address_detail = (JSONObject) content.get("address_detail");
        mCounty = address_detail.getString("district");
        mCity = address_detail.getString("city");
        mCityCode = address_detail.getString("city_code");
        mProvince = address_detail.getString("province");
        CLog.d(TAG, "city = " + mCity);
        CLog.d(TAG, "city_code = " + mCityCode);
        CLog.d(TAG, "province = " + mProvince);
        ret = true;
        return ret;
    }
}
