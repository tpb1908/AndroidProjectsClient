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
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
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
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.pddstudio.highlightjs.HighlightJsView;
import com.pddstudio.highlightjs.models.Theme;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Scanner;

public class HtmlTextView extends JellyBeanSpanFixTextView {

    public static final String TAG = "HtmlTextView";
    public static final boolean DEBUG = false;

    boolean linkHit;
    @Nullable
    private ClickableTableSpan clickableTableSpan;
    @Nullable
    private DrawTableLinkSpan drawTableLinkSpan;

    private final boolean dontConsumeNonUrlClicks = true;
    private boolean removeFromHtmlSpace = true;

    private boolean showUnderLines = true;

    private LinkClickHandler mLinkHandler;

    private ImageClickHandler mImageClickHandler;
    private final HashMap<String, Drawable> mDrawables = new HashMap<>();

    private CodeClickHandler mCodeHandler;

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

    public void setCodeClickHandler(CodeClickHandler handler) {
        mCodeHandler = handler;
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
    private void setHtml(@RawRes int resId, @Nullable Html.ImageGetter imageGetter) {
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
                mDrawables.clear();
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


                final SpannableString buffer = new SpannableString(text);
                final URLSpan[] spans = buffer.getSpans(0, buffer.length(), URLSpan.class);

                Linkify.addLinks(buffer, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);

                //Copy back the spans from the original text
                for(URLSpan us : spans) {
                    final int start = text.getSpanStart(us);
                    final int end = text.getSpanEnd(us);
                    buffer.setSpan(us, start, end, 0);
                }

                if(!showUnderLines) {
                    stripUnderLines(buffer);
                }

                if(mImageClickHandler != null) {
                    enableImageClicks(buffer);
                }

                if(mCodeHandler != null) {
                    enableCodeClicks(html, buffer);
                }

                HtmlTextView.this.post(new Runnable() {
                    @Override
                    public void run() {
                        setText(buffer);
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

    void addDrawable(Drawable drawable, String source) {
        mDrawables.put(source, drawable);
    }

    private void stripUnderLines(Spannable s) {
        final URLSpan[] spans = s.getSpans(0, s.length(), URLSpan.class);
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
                        Log.i(TAG, "onClick: Source is " + span.getSource());
                        if(mDrawables.containsKey(span.getSource())) {
                            mImageClickHandler.imageClicked(mDrawables.get(span.getSource()));
                        }
                    }
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                }
            }, s.getSpanStart(span), s.getSpanEnd(span), s.getSpanFlags(span));
        }
    }

    private void enableCodeClicks(String text, final Spannable s) {
        final CodeSpan[] spans = s.getSpans(0, s.length(), CodeSpan.class);
        int startIndex = 0;
        int endIndex = 0;
        int i = 0;
        while(startIndex != -1 && i < spans.length) {
            startIndex = text.indexOf("<code>", startIndex);
            if(startIndex == -1) break;
            endIndex = text.indexOf("</code>", startIndex);
            if(endIndex != -1) {
                spans[i].setCode(text.substring(startIndex + "<code>".length(), endIndex));
                spans[i].setHandler(mCodeHandler);
            }
            i++;
            startIndex = endIndex;
        }
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

        URLSpanWithoutUnderline(Parcel in) {
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

    public static class CodeSpan extends ClickableSpan {
        private CodeClickHandler mHandler;
        private String mCode;


        void setHandler(CodeClickHandler handler) {
            mHandler = handler;
        }

        void setCode(String code) {
            mCode = code;
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
            ds.setTypeface(Typeface.DEFAULT_BOLD);
        }

        @Override
        public void onClick(View widget) {
            if(mHandler != null) {
                mHandler.codeClicked(mCode);
            }
        }


    }

    public interface LinkClickHandler {

        void onClick(String url);

    }

    public interface ImageClickHandler {

        void imageClicked(Drawable drawable);

    }

    public interface CodeClickHandler {

        void codeClicked(String code);

    }

    public static class ImageDialog implements ImageClickHandler {

        private final Context mContext;

        public ImageDialog(Context context) {
            mContext = context;
        }

        @Override
        public void imageClicked(Drawable drawable) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            final LayoutInflater inflater = LayoutInflater.from(mContext);
            final View view = inflater.inflate(R.layout.dialog_image, null);

            builder.setView(view);

            final FillingImageView fiv = (FillingImageView) view.findViewById(R.id.dialog_imageview);
            fiv.setImageDrawable(drawable);

            final Dialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);

            fiv.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();

                }
            });

            dialog.show();

        }
    }

    public static class CodeDialog implements CodeClickHandler {

        private Context mContext;

        public CodeDialog(Context context) {
            mContext = context;
        }

        @Override
        public void codeClicked(String code) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            final LayoutInflater inflater = LayoutInflater.from(mContext);
            final View view = inflater.inflate(R.layout.dialog_code, null);

            builder.setView(view);

            final HighlightJsView wv = (HighlightJsView) view.findViewById(R.id.dialog_highlight_view);
            wv.setTheme(Theme.ANDROID_STUDIO);
            wv.setSource(code);
            final Dialog dialog = builder.create();

            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            dialog.show();
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
