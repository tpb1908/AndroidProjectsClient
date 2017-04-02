package com.tpb.mdtext.views.spans;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.view.View;

import com.tpb.mdtext.handlers.LinkClickHandler;

/**
 * Created by theo on 27/02/17.
 */

public class CleanURLSpan extends URLSpan {
    private LinkClickHandler mHandler;

    public CleanURLSpan(String url) {
        super(ensureValidUrl(url));
    }

    public CleanURLSpan(String url, LinkClickHandler handler) {
        super(ensureValidUrl(url));
        mHandler = handler;
    }

    @Override
    public void onClick(View widget) {
        if(mHandler == null) {
            final Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(getURL()));
            widget.getContext().startActivity(i);
        } else {
            mHandler.onClick(getURL());
        }
    }

    private static String ensureValidUrl(@Nullable String url) {
        if(url == null) return null;
        if(!url.startsWith("https://") && !url.startsWith("http://")) {
            return "http://" + url;
        }
        return url;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        super.updateDrawState(ds);
        // Links are bold without underline
        ds.setUnderlineText(false);
        ds.setTypeface(Typeface.DEFAULT_BOLD);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    CleanURLSpan(Parcel in) {
        super(in);
    }

    public static final Creator<CleanURLSpan> CREATOR = new Creator<CleanURLSpan>() {
        @Override
        public CleanURLSpan createFromParcel(Parcel source) {
            return new CleanURLSpan(source);
        }

        @Override
        public CleanURLSpan[] newArray(int size) {
            return new CleanURLSpan[size];
        }
    };
}
