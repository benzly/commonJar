package com.letv.commonjar.widget;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.benz.fastblur.Fast2Blur;
import com.letv.commonjar.CLog;

import miui.util.ImageUtils;

public class StyleDialog extends Dialog {

    private static final String TAG = CLog.makeTag(StyleDialog.class);
    private int mBlurLevel = 10;
    private View mViewToBlur;
    private ImageView mViewBeBlur;
    private int mAniamtionPanelId;
    private View mAnimationPanel;
    private int mAnimationTime = 300;
    private View mShadowView;
    private View mDefaultFocusView;

    private Bitmap mPrepareBlurBitmap;

    private AnimationSet mInAnimation, mOutAnimation, mInBlurAnimation, mOutBlurAnimation, mOutShadowAnimation;

    public StyleDialog(Context context) {
        super(context);
    }

    public StyleDialog(Context context, int theme) {
        super(context, theme);
    }

    public StyleDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mAniamtionPanelId > 0 && mAnimationPanel == null) {
            mAnimationPanel = findViewById(mAniamtionPanelId);
        }
    }

    public void setAnimationPanel(int viewId) {
        this.setAnimationPanel(viewId, mAnimationTime);
    }

    public void setAnimationPanel(int viewId, int duration) {
        this.mAniamtionPanelId = viewId;
        this.mAnimationTime = duration;
    }

    public void setAnimationPanel(View v) {
        this.setAnimationPanel(v, mAnimationTime);
    }

    public void setAnimationPanel(View v, int duration) {
        this.mAnimationPanel = v;
        this.mAnimationTime = duration;
    }

    public void setBlurView(View v) {
        this.setBlurView(v, 10);
    }

    public void setBlurView(View view, int blurLevel) {
        if (blurLevel < 0 || blurLevel > 50) {
            throw new IllegalArgumentException("blurLevel must be  >=0 & <=50");
        }
        mViewToBlur = view;
        mBlurLevel = blurLevel;
    }

    public boolean prepareBlur() {
        boolean ret = false;
        try {
            if (mViewToBlur != null) {
                Bitmap localBitmap1 = Bitmap.createBitmap(mViewToBlur.getWidth(), mViewToBlur.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas localCanvas = new Canvas(localBitmap1);
                mViewToBlur.draw(localCanvas);
                Bitmap localBitmap2 = Bitmap.createScaledBitmap(localBitmap1, 480, 270, true);
                CLog.d(TAG, "onStart blur image width = " + localBitmap2.getWidth() + " height = " + localBitmap2.getHeight());
                mPrepareBlurBitmap = Bitmap.createBitmap(localBitmap2.getWidth(), localBitmap2.getHeight(), Bitmap.Config.ARGB_8888);
                mPrepareBlurBitmap.eraseColor(Color.argb(224, 0, 0, 0));
                ImageUtils.getInstance().createFastBlur(localBitmap2, mPrepareBlurBitmap, mBlurLevel);
                ret = true;
            }
        } catch (Exception e) {
            ret = false;
            CLog.d(TAG, "prepareBlur error=" + e.toString());
        }
        return ret;
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        mShadowView = view;
    }

    @Override
    public void show() {
        super.show();
        if (mAnimationPanel != null) {
            mAnimationPanel.startAnimation(getInAnimation());
        }
        if (mViewBeBlur != null) {
            mViewBeBlur.startAnimation(getInBlurAnimation());
        }
        if (mInAnimation != null && mShadowView != null) {
            ColorDrawable begin = new ColorDrawable(Color.parseColor("#00000000"));
            ColorDrawable end = new ColorDrawable(Color.argb(180, 0, 0, 0));
            TransitionDrawable td = new TransitionDrawable(new Drawable[] {begin, end});
            td.startTransition(mAnimationTime);
            mShadowView.setBackgroundDrawable(td);
        }
        if (mDefaultFocusView != null) {
            mDefaultFocusView.requestFocus();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean ret = false;
        if (keyCode == KeyEvent.KEYCODE_ESCAPE || keyCode == KeyEvent.KEYCODE_BACK) {
            dismissByAnimation();
            ret = true;
        }
        return ret;
    }

    public void dismissByAnimation() {
        boolean hasAnimation = false;
        if (mAnimationPanel != null) {
            hasAnimation = true;
            mAnimationPanel.startAnimation(getOutAnimation());
        }
        if (mViewBeBlur != null) {
            hasAnimation = true;
            mViewBeBlur.startAnimation(getOutBlurAnimation());
        }
        if (hasAnimation) {
            if (mShadowView != null) {
                mShadowView.startAnimation(getOutShadowAnimation());
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    StyleDialog.this.dismiss();
                }
            }, mAnimationTime);
        } else {
            this.dismiss();
        }
    }

    @Override
    public void dismiss() {
        super.dismiss();
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            if (mViewToBlur == null) {
                return;
            }
            Bitmap localBitmap3 = null;
            if (mPrepareBlurBitmap == null) {
                Bitmap localBitmap1 = Bitmap.createBitmap(mViewToBlur.getWidth(), mViewToBlur.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas localCanvas = new Canvas(localBitmap1);
                mViewToBlur.draw(localCanvas);
                Bitmap localBitmap2 = Bitmap.createScaledBitmap(localBitmap1, 480, 270, true);
                CLog.d(TAG, "onStart blur image width = " + localBitmap2.getWidth() + " height = " + localBitmap2.getHeight());
                localBitmap3 = Bitmap.createBitmap(localBitmap2.getWidth(), localBitmap2.getHeight(), Bitmap.Config.ARGB_8888);
                localBitmap3.eraseColor(Color.argb(224, 0, 0, 0));
                ImageUtils.getInstance().createFastBlur(localBitmap2, localBitmap3, mBlurLevel);
//                Fast2Blur.build(localBitmap3, mBlurLevel);
            } else {
                localBitmap3 = mPrepareBlurBitmap;
            }

            mViewBeBlur = new ImageView(getContext());
            mViewBeBlur.setImageBitmap(localBitmap3);
            mViewBeBlur.setScaleType(ScaleType.FIT_XY);
            ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
            decorView.addView(mViewBeBlur, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        } catch (Exception e) {
            CLog.e(TAG, "createBlur", e);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private AnimationSet getInAnimation() {
        if (mInAnimation == null) {
            mInAnimation = new AnimationSet(true);
            AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
            ScaleAnimation scaleAnimation = //
                    new ScaleAnimation(1.2f, 1.0f, 1.2f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            mInAnimation.addAnimation(scaleAnimation);
            mInAnimation.addAnimation(alphaAnimation);
            mInAnimation.setDuration(mAnimationTime);
        }
        return mInAnimation;
    }

    private AnimationSet getInBlurAnimation() {
        if (mInBlurAnimation == null) {
            mInBlurAnimation = new AnimationSet(true);
            AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
            mInBlurAnimation.addAnimation(alphaAnimation);
            mInBlurAnimation.setDuration(mAnimationTime);
        }
        return mInBlurAnimation;
    }

    private AnimationSet getOutAnimation() {
        if (mOutAnimation == null) {
            mOutAnimation = new AnimationSet(true);
            AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
            ScaleAnimation scaleAnimation = //
                    new ScaleAnimation(1.0f, 1.2f, 1.0f, 1.2f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            alphaAnimation.setDuration(mAnimationTime);
            scaleAnimation.setDuration(mAnimationTime);
            mOutAnimation.addAnimation(scaleAnimation);
            mOutAnimation.addAnimation(alphaAnimation);
            mOutAnimation.setFillEnabled(true);
            mOutAnimation.setFillAfter(true);
        }
        return mOutAnimation;
    }

    private AnimationSet getOutBlurAnimation() {
        if (mOutBlurAnimation == null) {
            mOutBlurAnimation = new AnimationSet(true);
            AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
            mOutBlurAnimation.addAnimation(alphaAnimation);
            mOutBlurAnimation.setFillEnabled(true);
            mOutBlurAnimation.setFillAfter(true);
            mOutBlurAnimation.setDuration(mAnimationTime);
        }
        return mOutBlurAnimation;
    }

    private AnimationSet getOutShadowAnimation() {
        if (mOutShadowAnimation == null) {
            mOutShadowAnimation = new AnimationSet(true);
            AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
            mOutShadowAnimation.addAnimation(alphaAnimation);
            mOutShadowAnimation.setFillEnabled(true);
            mOutShadowAnimation.setFillAfter(true);
            mOutShadowAnimation.setDuration(mAnimationTime);
        }
        return mOutShadowAnimation;
    }
}
