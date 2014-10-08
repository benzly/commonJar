/**
 * =====================================================================
 *
 * @file  JTypeFaceMgr.java
 * @Module Name   com.joysee.common.widget
 * @author YueLiang
 * @OS version  1.0
 * @Product type: JoySee
 * @date   2013年10月28日
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
 * YueLiang          2013年10月28日           1.0         Check for NULL, 0 h/w
 * =====================================================================
 **/
//

package com.letv.commonjar.widget;

import android.content.Context;
import android.graphics.Typeface;

import java.util.HashMap;

public class TypeFaceMgr {
	
	public TypeFaceMgr() {
		
	}
	
    HashMap<String, Typeface> mTypefaces = new HashMap<String, Typeface>();

    public Typeface getTypeface(Context context, String paramString) {
        Typeface localObject = null;
        if (this.mTypefaces.containsKey(paramString)) {
            localObject = this.mTypefaces.get(paramString);
        }

        if (localObject == null) {
            try {
                Typeface localTypeface = Typeface.createFromAsset(context.getAssets(),
                        "fonts/" + paramString);
                this.mTypefaces.put(paramString, localTypeface);
                localObject = localTypeface;
            } catch (Exception localException) {
                localException.printStackTrace();
                localObject = null;
            }
        }
        return localObject;
    }
}
