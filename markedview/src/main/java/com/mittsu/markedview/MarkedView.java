package com.mittsu.markedview;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * The MarkedView is the Markdown viewer.
 * <p>
 * Created by mittsu on 2016/04/25.
 */
public final class MarkedView extends WebView implements NestedScrollingChild {

    private static final String TAG = MarkedView.class.getSimpleName();

    private int mLastY;
    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private int mNestedOffsetY;
    private NestedScrollingChildHelper mChildHelper;
    private boolean mInterceptTouchEvent = false;

    private SwipeRefreshLayout mParent;

    private String previewText;
    private boolean codeScrollDisable = false;
    private boolean darkTheme = false;

    public MarkedView(Context context) {
        this(context, null);
    }

    public MarkedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MarkedView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
        init();
    }

    @TargetApi(11)
    @SuppressLint("SetJavaScriptEnabled")
    private void init() {
        setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    loadUrl(previewText);
                } else {
                    evaluateJavascript(previewText, null);
                }
            }
        });
        addJavascriptInterface(this, "TouchIntercept");
        if(darkTheme) {
            loadUrl("file:///android_asset/html/md_preview_dark.html");
        } else {
            loadUrl("file:///android_asset/html/md_preview.html");
        }

        getSettings().setJavaScriptEnabled(true);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            getSettings().setAllowUniversalAccessFromFileURLs(true);
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
    }

    public void setParent(SwipeRefreshLayout parent) {
        mParent = parent;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @JavascriptInterface
    public void beginTouchIntercept() {
        mInterceptTouchEvent = true;
    }

    @JavascriptInterface
    public void endTouchIntercept() {
        mInterceptTouchEvent = false;
    }

    public void setMarkdown(String mdText) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            previewText = String
                    .format("javascript:preview('%s', %b)", escape(mdText), isCodeScrollDisable());
        } else {
            previewText = String.format("preview('%s', %b)", escape(mdText), isCodeScrollDisable());
        }
    }

    private String escape(String mdText) {
        String escText = mdText.replace("\n", "\\\\n");
        escText = escText.replace("'", "\\\'");
        //in some cases the string may have "\r" and our view will show nothing,so replace it
        escText = escText.replace("\r", "");
        return escText;
    }

    /* options */

    public void setCodeScrollDisable() {
        codeScrollDisable = true;
    }

    private boolean isCodeScrollDisable() {
        return codeScrollDisable;
    }

    public void enableDarkTheme() {
        darkTheme = true;
        init();
    }

    public void disableDarkTheme() {
        darkTheme = false;
        init();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if(mInterceptTouchEvent && getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(mInterceptTouchEvent);
            return super.onTouchEvent(ev);
        }

        boolean rv = false;

        MotionEvent event = MotionEvent.obtain(ev);
        final int action = MotionEventCompat.getActionMasked(event);
        if(action == MotionEvent.ACTION_DOWN) {
            mNestedOffsetY = 0;
        }
        int eventY = (int) event.getY();
        event.offsetLocation(0, mNestedOffsetY);
        switch(action) {
            case MotionEvent.ACTION_MOVE:
                int deltaY = mLastY - eventY;
                // NestedPreScroll
                if(dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset)) {
                    deltaY -= mScrollConsumed[1];
                    mLastY = eventY - mScrollOffset[1];
                    event.offsetLocation(0, -mScrollOffset[1]);
                    mNestedOffsetY += mScrollOffset[1];
                }
                rv = super.onTouchEvent(event);

                // NestedScroll
                if(dispatchNestedScroll(0, mScrollOffset[1], 0, deltaY, mScrollOffset)) {
                    event.offsetLocation(0, mScrollOffset[1]);
                    mNestedOffsetY += mScrollOffset[1];
                    mLastY -= mScrollOffset[1];
                }
                break;
            case MotionEvent.ACTION_DOWN:
                rv = super.onTouchEvent(event);
                mLastY = eventY;
                // start NestedScroll
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                rv = super.onTouchEvent(event);
                // end NestedScroll
                stopNestedScroll();
                break;
        }
        return rv;
    }

    // Nested Scroll implements
    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed,
                                        int[] offsetInWindow) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                offsetInWindow
        );
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

}
