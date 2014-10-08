/**
 * =====================================================================
 *
 * @file JTextViewWithTTF.java
 * @Module Name com.joysee.common.widget
 * @author YueLiang
 * @OS version 1.0
 * @Product type: JoySee
 * @date 2013年10月28日
 * @brief This file is the http **** implementation.
 * @This file is responsible by ANDROID TEAM.
 * @Comments: ===================================================================== Revision
 *            History:
 *
 *            Modification Tracking
 *
 *            Author Date OS version Reason ---------- ------------ ------------- -----------
 *            YueLiang 2013年10月28日 1.0 Check for NULL, 0 h/w
 *            =====================================================================
 **/
//

package com.letv.commonjar.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;
import com.letv.commonjar.R;

public class TextViewWithTTF extends TextView {

    private Context mContext;
    protected static final TypeFaceMgr SFontMgr = new TypeFaceMgr();

    public TextViewWithTTF(Context context, AttributeSet attr) {
        this(context, attr, 0);
    }



    public TextViewWithTTF(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        TypedArray localTypedArray = context.obtainStyledAttributes(attrs, R.styleable.TextViewWithTTF);
        setTypeface(SFontMgr.getTypeface(mContext, localTypedArray.getString(R.styleable.TextViewWithTTF_ttf)));
        localTypedArray.recycle();
    }



    public TextViewWithTTF(Context context, String s) {
        super(context);
        mContext = context;
        setTypeface(SFontMgr.getTypeface(mContext, s));
    }

    public void draw(Canvas c) {
        super.draw(c);
    }

    public void setTypeface(Typeface typeface) {
        super.setTypeface(typeface);
    }

}
