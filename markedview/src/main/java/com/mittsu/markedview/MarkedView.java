package com.mittsu.markedview;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * The MarkedView is the Markdown viewer.
 *
 * Created by mittsu on 2016/04/25.
 */
public final class MarkedView extends WebView {

    private static final String TAG = MarkedView.class.getSimpleName();

    private String previewText;
    private boolean codeScrollDisable = true;
    private boolean darkTheme = false;

    public MarkedView(Context context) {
        this(context, null);
    }

    public MarkedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MarkedView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(11)
    @SuppressLint("SetJavaScriptEnabled")
    private void init(){
        setWebViewClient(new WebViewClient(){
            public void onPageFinished(WebView view, String url){
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    loadUrl(previewText);
                } else {
                    evaluateJavascript(previewText, null);
                }
            }
        });
        if(darkTheme) {
            loadUrl("file:///android_asset/html/md_preview_dark.html");
        } else {
            loadUrl("file:///android_asset/html/md_preview.html");
        }

        getSettings().setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            getSettings().setAllowUniversalAccessFromFileURLs(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
    }

    public void setMarkdown(String mdText){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            previewText = String.format("javascript:preview('%s', %b)", escape(mdText), isCodeScrollDisable());
        } else {
            previewText = String.format("preview('%s', %b)", escape(mdText), isCodeScrollDisable());
        }
        Log.i(MarkedView.class.getSimpleName(), "Text: " + previewText);
    }

    private String escape(String mdText){
        String escText = mdText.replace("\n", "\\\\n");
        escText = escText.replace("'", "\\\'");
        //in some cases the string may have "\r" and our view will show nothing,so replace it
        escText = escText.replace("\r","");
        return escText;
    }

    /* options */

    public void setCodeScrollDisable(){
        codeScrollDisable = true;
    }

    private boolean isCodeScrollDisable(){
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

}
