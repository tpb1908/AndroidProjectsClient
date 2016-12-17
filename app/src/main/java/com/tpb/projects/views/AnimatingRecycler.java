package com.tpb.projects.views;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by theo on 17/12/16.
 */

public class AnimatingRecycler extends RecyclerView {
    private boolean mIsScrollable;
    private boolean mShouldAnimate = true;

    public AnimatingRecycler(Context context) {
        this(context, null);
    }

    public AnimatingRecycler(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnimatingRecycler(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mIsScrollable = false;
    }

    public void enableAnimation() {
        mShouldAnimate = true;
    }

    public void disableAnimation() {
        mShouldAnimate = false;
    }

    public boolean shouldAnimate() {
        return mShouldAnimate;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return !mIsScrollable || super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if(mShouldAnimate) {
            for(int i = 0; i < getChildCount(); i++) {
                animate(getChildAt(i), i);

                if(i == getChildCount() - 1) {
                    getHandler().postDelayed(() -> mIsScrollable = true, i * 100);
                }
            }
        }
    }

    private void animate(View view, final int pos) {
        view.animate().cancel();
        view.setTranslationY(100);
        view.setAlpha(0);
        view.animate().alpha(1.0f).translationY(0).setDuration(300).setStartDelay(pos * 70);
    }

}
