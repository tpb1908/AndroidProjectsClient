package com.tpb.projects.common;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.tpb.mdtext.handlers.NestedScrollHandler;

/**
 * Created by theo on 15/04/17.
 */

public class LockableViewPager extends ViewPager implements NestedScrollHandler {

    private boolean isPagingEnabled = true;

    public LockableViewPager(Context context) {
        super(context);
    }

    public LockableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return this.isPagingEnabled && super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return this.isPagingEnabled && super.onInterceptTouchEvent(event);
    }

    @Override
    public void onScrollLocked() {
        setPagingEnabled(false);
    }

    @Override
    public void onScrollUnlocked() {
        setPagingEnabled(true);
    }

    public void setPagingEnabled(boolean b) {
        this.isPagingEnabled = b;
    }

}
