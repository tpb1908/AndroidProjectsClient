package org.sufficientlysecure.htmltext;

import android.graphics.Typeface;
import android.os.Parcel;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.view.View;

import org.sufficientlysecure.htmltext.handlers.LinkClickHandler;

/**
 * Created by theo on 27/02/17.
 */

public class CleanURLSpan extends URLSpan {
    private LinkClickHandler mHandler;

    public CleanURLSpan(String url) {
        super(url);
    }

    public CleanURLSpan(String url, LinkClickHandler handler) {
        super(url);
        mHandler = handler;
    }

    @Override
    public void onClick(View widget) {
        if(mHandler == null) {
            super.onClick(widget);
        } else {
            mHandler.onClick(getURL());
        }
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
