/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.htmltextview;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.io.InputStream;
import java.util.Scanner;

public class HtmlTextView extends JellyBeanSpanFixTextView {

    public static final String TAG = "HtmlTextView";
    public static final boolean DEBUG = false;

    boolean linkHit;
    @Nullable
    private ClickableTableSpan clickableTableSpan;
    @Nullable
    private DrawTableLinkSpan drawTableLinkSpan;

    boolean dontConsumeNonUrlClicks = true;
    private boolean removeFromHtmlSpace = true;

    private boolean showUnderLines = true;

    private LinkClickHandler mLinkHandler;

    private ImageClickHandler mImageClickHandler;

    private Handler mParseHandler;

    public HtmlTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public HtmlTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HtmlTextView(Context context) {
        super(context);
    }

    public void setLinkHandler(LinkClickHandler handler) {
        mLinkHandler = handler;
    }

    public void setParseHandler(Handler parseHandler) {
        mParseHandler = parseHandler;
    }

    public void setImageHandler(ImageClickHandler imageHandler) {
        mImageClickHandler = imageHandler;
    }

    /**
     * @see org.sufficientlysecure.htmltextview.HtmlTextView#setHtml(int)
     */
    public void setHtml(@RawRes int resId) {
        setHtml(resId, null);
    }

    /**
     * @see org.sufficientlysecure.htmltextview.HtmlTextView#setHtml(String)
     */
    public void setHtml(@NonNull String html) {
        setHtml(html, null);
    }

    /**
     * Loads HTML from a raw resource, i.e., a HTML file in res/raw/.
     * This allows translatable resource (e.g., res/raw-de/ for german).
     * The containing HTML is parsed to Android's Spannable format and then displayed.
     *
     * @param resId       for example: R.raw.help
     * @param imageGetter for fetching images. Possible ImageGetter provided by this library:
     *                    HtmlLocalImageGetter and HtmlRemoteImageGetter
     */
    public void setHtml(@RawRes int resId, @Nullable Html.ImageGetter imageGetter) {
        InputStream inputStreamText = getContext().getResources().openRawResource(resId);

        setHtml(convertStreamToString(inputStreamText), imageGetter);
    }

    /**
     * Parses String containing HTML to Android's Spannable format and displays it in this TextView.
     * Using the implementation of Html.ImageGetter provided.
     *
     * @param html        String containing HTML, for example: "<b>Hello world!</b>"
     * @param imageGetter for fetching images. Possible ImageGetter provided by this library:
     *                    HtmlLocalImageGetter and HtmlRemoteImageGetter
     */
    //http://stackoverflow.com/a/17201376/4191572
    public void setHtml(@NonNull final String html, @Nullable final Html.ImageGetter imageGetter) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                final HtmlTagHandler htmlTagHandler = new HtmlTagHandler();
                htmlTagHandler.setClickableTableSpan(clickableTableSpan);
                htmlTagHandler.setDrawTableLinkSpan(drawTableLinkSpan);

                final String overridden = htmlTagHandler.overrideTags(html);

                final Spanned text;
                if(removeFromHtmlSpace) {
                    text = removeHtmlBottomPadding(Html.fromHtml(overridden, imageGetter, htmlTagHandler));
                } else {
                    text = Html.fromHtml(overridden, imageGetter, htmlTagHandler);
                }

                final URLSpan[] spans = text.getSpans(0, text.length(), URLSpan.class);

                final SpannableString buffer = new SpannableString(text);
                Linkify.addLinks(buffer, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);

                for(URLSpan us : spans) {
                    final int end = text.getSpanEnd(us);
                    final int start = text.getSpanStart(us);
                    buffer.setSpan(
                            showUnderLines ? us : mLinkHandler == null ? new URLSpanWithoutUnderline(us.getURL()) : new URLSpanWithoutUnderline(us.getURL(), mLinkHandler),
                            start, end, 0);
                }

                if(!showUnderLines) {
                    stripUnderLines(buffer);
                }

                HtmlTextView.this.post(new Runnable() {
                    @Override
                    public void run() {
                        setText(buffer);
                        if(mImageClickHandler != null) {
                            enableImageClicks(buffer);
                        }
                        // make links work
                        setMovementMethod(LocalLinkMovementMethod.getInstance());
                    }
                });
            }
        };
        if(mParseHandler != null) {
            mParseHandler.postDelayed(r, 20);
        } else {
            r.run();
        }

    }

    private void stripUnderLines(Spannable s) {
        URLSpan[] spans = s.getSpans(0, s.length(), URLSpan.class);
        for(URLSpan span : spans) {
            final int start = s.getSpanStart(span);
            final int end = s.getSpanEnd(span);
            s.removeSpan(span);
            if(mLinkHandler == null) {
                span = new URLSpanWithoutUnderline(span.getURL());
            } else {
                span = new URLSpanWithoutUnderline(span.getURL(), mLinkHandler);
            }
            s.setSpan(span, start, end, 0);
        }
    }

    private void enableImageClicks(final Spannable s) {
        for(final ImageSpan span : s.getSpans(0, s.length(), ImageSpan.class)) {
            s.setSpan(new URLSpan(span.getSource()) {
                @Override
                public void onClick(View widget) {
                    if(mImageClickHandler == null) {
                        super.onClick(widget); //Opens image link
                    } else {
                        mImageClickHandler.imageClicked(span.getDrawable());
                    }
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                }
            }, s.getSpanStart(span), s.getSpanEnd(span), s.getSpanFlags(span));
        }
        setText(s);
    }

    private static class URLSpanWithoutUnderline extends URLSpan {
        private LinkClickHandler mHandler;

        URLSpanWithoutUnderline(String url) {
            super(url);
        }

        URLSpanWithoutUnderline(String url, LinkClickHandler handler) {
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

        protected URLSpanWithoutUnderline(Parcel in) {
            super(in);
        }

        public static final Creator<URLSpanWithoutUnderline> CREATOR = new Creator<URLSpanWithoutUnderline>() {
            @Override
            public URLSpanWithoutUnderline createFromParcel(Parcel source) {
                return new URLSpanWithoutUnderline(source);
            }

            @Override
            public URLSpanWithoutUnderline[] newArray(int size) {
                return new URLSpanWithoutUnderline[size];
            }
        };
    }

    public interface LinkClickHandler {

        void onClick(String url);

    }

    public interface ImageClickHandler {

        void imageClicked(Drawable drawable);

    }

    public static class ImageDialog implements ImageClickHandler {

        private Context mContext;

        public ImageDialog(Context context) {
            mContext = context;
        }

        @Override
        public void imageClicked(Drawable drawable) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            final LayoutInflater inflater = LayoutInflater.from(mContext);
            final View view = inflater.inflate(R.layout.dialog_image, null);

            builder.setView(view);

            ((ImageView)view.findViewById(R.id.dialog_imageview)).setImageDrawable(drawable);
            builder.create().show();


//            final Dialog builder = new Dialog(mContext);
//            builder.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//            builder.setOnDismissListener(null);
//
//            final ImageView iv = new ImageView(mContext);
//            iv.setAdjustViewBounds(true);
//            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
//            iv.setImageDrawable(drawable.getConstantState().newDrawable());
//
//            builder.addContentView(iv, new RelativeLayout.LayoutParams(
//                    ViewGroup.LayoutParams.MATCH_PARENT,
//                    ViewGroup.LayoutParams.WRAP_CONTENT));
//            Log.i(TAG, "imageClicked: Drawable " + drawable.getIntrinsicWidth() + ", " + drawable.getIntrinsicHeight());
//            Log.i(TAG, "imageClicked: Is drawable null? " + drawable + " " + (drawable == null));
//            //iv.setBackground(drawable);
//
//            builder.show();
        }
    }

    /**
     * Note that this must be called before setting text for it to work
     */
    public void setRemoveFromHtmlSpace(boolean removeFromHtmlSpace) {
        this.removeFromHtmlSpace = removeFromHtmlSpace;
    }

    public void setClickableTableSpan(@Nullable ClickableTableSpan clickableTableSpan) {
        this.clickableTableSpan = clickableTableSpan;
    }

    public void setDrawTableLinkSpan(@Nullable DrawTableLinkSpan drawTableLinkSpan) {
        this.drawTableLinkSpan = drawTableLinkSpan;
    }

    public void setShowUnderLines(boolean show) {
        showUnderLines = show;
    }

    /**
     * http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
     */
    @NonNull
    static private String convertStreamToString(@NonNull InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    /**
     * Html.fromHtml sometimes adds extra space at the bottom.
     * This methods removes this space again.
     * See https://github.com/SufficientlySecure/html-textview/issues/19
     */
    @Nullable
    static private Spanned removeHtmlBottomPadding(@Nullable Spanned text) {
        if (text == null) {
            return null;
        }

        while (text.length() > 0 && text.charAt(text.length() - 1) == '\n') {
            text = (Spanned) text.subSequence(0, text.length() - 1);
        }
        return text;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        linkHit = false;
        boolean res = super.onTouchEvent(event);

        if (dontConsumeNonUrlClicks) {
            return linkHit;
        }
        return res;
    }

}
