package com.tpb.animatingrecyclerview;

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
    private AnimationType anim = AnimationType.ALPHA;

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
        mIsScrollable = true;
    }

    public boolean shouldAnimate() {
        return mShouldAnimate;
    }

    public void setAnimationType(AnimationType anim) {
        this.anim = anim;
    }

    public AnimationType getAnimationType() {
        return anim;
    }

    private final Runnable enableScrolling = new Runnable() {
        @Override
        public void run() {
            mIsScrollable = true;
        }
    };

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return !mIsScrollable || super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if(mShouldAnimate) {
            for(int i = 0; i < getChildCount(); i++) {
                switch(anim) {
                    case HORIZONTAL:
                        animateHorizontal(getChildAt(i), i);
                        break;
                    case VERTICAL:
                        animateVertical(getChildAt(i), i);
                        break;
                    case ALPHA:
                        animateAlpha(getChildAt(i), i);
                        break;
                }

                if(i == getChildCount() - 1) {
                    getHandler().postDelayed(enableScrolling, i * 100);
                }
            }
        } else {
            mIsScrollable = true;
        }
        mShouldAnimate = false;
    }

    private void animateHorizontal(View view, final int pos) {
        view.animate().cancel();
        view.setTranslationX(view.getWidth());
        view.animate().translationX(0).setDuration(300).setStartDelay(pos * 70);
    }

    private void animateVertical(View view, final int pos) {
        view.animate().cancel();
        view.setTranslationY(100);
        view.setAlpha(0);
        view.animate().alpha(1.0f).translationY(0).setDuration(300).setStartDelay(pos * 70);
    }

    private void animateAlpha(View view, final int pos) {
        view.animate().cancel();
        view.setAlpha(0.0f);
        view.animate().alpha(1.0f).setDuration(300).setStartDelay(pos * 70);
    }

    public enum AnimationType {

        HORIZONTAL, VERTICAL, ALPHA

    }

}
