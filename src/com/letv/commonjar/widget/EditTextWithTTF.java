/**
 * =====================================================================
 *
 * @file  JEditTextWithTTF.java
 * @Module Name   com.joysee.common.widget
 * @author benz
 * @OS version  1.0
 * @Product type: JoySee
 * @date   2014-1-21
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
 * benz          2014-1-21           1.0         Check for NULL, 0 h/w
 * =====================================================================
 **/

package com.letv.commonjar.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.EditText;

import com.letv.commonjar.R;

public class EditTextWithTTF extends EditText {

	private Context mContext;
	protected static final TypeFaceMgr SFontMgr = new TypeFaceMgr();

	public EditTextWithTTF(Context context) {
		this(context, null);
	}

	public EditTextWithTTF(Context context, AttributeSet attr) {
		this(context, attr, 0);
	}

	public EditTextWithTTF(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		TypedArray localTypedArray = context.obtainStyledAttributes(attrs, R.styleable.TextViewWithTTF);
		setTypeface(SFontMgr.getTypeface(mContext, localTypedArray.getString(R.styleable.TextViewWithTTF_ttf)));
		localTypedArray.recycle();
	}

}
