/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.letv.commonjar.http;

import android.os.Build;
import android.os.StrictMode;

/**
 * Class containing some static utility methods.
 */
class CacheUtils {
    private CacheUtils() {};

    public static void enableStrictMode(Class<?> c) {
        if (CacheUtils.hasGingerbread()) {
            StrictMode.ThreadPolicy.Builder threadPolicyBuilder = //
            		new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog();
            
            StrictMode.VmPolicy.Builder vmPolicyBuilder = //
            		new StrictMode.VmPolicy.Builder().detectAll().penaltyLog();

            if (CacheUtils.hasHoneycomb()) {
                threadPolicyBuilder.penaltyFlashScreen();
                vmPolicyBuilder.setClassInstanceLimit(c, 1);
            }
            StrictMode.setThreadPolicy(threadPolicyBuilder.build());
            StrictMode.setVmPolicy(vmPolicyBuilder.build());
        }
    }
    

    /**
     * @return 是否在Android 2.2 或以上
     */
    public static boolean hasFroyo() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    /**
     * @return 是否在Android 2.3 或以上
     */
    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    /**
     * @return 是否在Android 3.0 或以上
     */
    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    /**
     * @return 是否在Android 3.1 或以上
     */
    public static boolean hasHoneycombMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    }

    /**
     * @return 是否在Android 4.1 或以上
     */
    public static boolean hasJellyBean() {
//    	return false;
        return Build.VERSION.SDK_INT >= 16;//Build.VERSION_CODES.JELLY_BEAN;
    }
}
