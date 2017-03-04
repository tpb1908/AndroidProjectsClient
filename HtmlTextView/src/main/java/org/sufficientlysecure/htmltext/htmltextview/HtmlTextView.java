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

package org.sufficientlysecure.htmltext.htmltextview;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
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
import android.view.MotionEvent;
import android.view.View;

import org.sufficientlysecure.htmltext.CleanURLSpan;
import org.sufficientlysecure.htmltext.ClickableTableSpan;
import org.sufficientlysecure.htmltext.CodeSpan;
import org.sufficientlysecure.htmltext.DrawTableLinkSpan;
import org.sufficientlysecure.htmltext.HtmlHttpImageGetter;
import org.sufficientlysecure.htmltext.HtmlTagHandler;
import org.sufficientlysecure.htmltext.LocalLinkMovementMethod;
import org.sufficientlysecure.htmltext.handlers.CodeClickHandler;
import org.sufficientlysecure.htmltext.handlers.ImageClickHandler;
import org.sufficientlysecure.htmltext.handlers.LinkClickHandler;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Scanner;

public class HtmlTextView extends JellyBeanSpanFixTextView implements HtmlHttpImageGetter.DrawableCacheHandler {

    public static final String TAG = HtmlTextView.class.getSimpleName();
    public static final boolean DEBUG = false;

    public boolean linkHit;
    @Nullable
    private ClickableTableSpan clickableTableSpan;
    @Nullable
    private DrawTableLinkSpan drawTableLinkSpan;

    private final boolean dontConsumeNonUrlClicks = true;
    private boolean removeFromHtmlSpace = true;

    private boolean showUnderLines = false;

    private LinkClickHandler mLinkHandler;

    private ImageClickHandler mImageClickHandler;
    private final HashMap<String, Drawable> mDrawables = new HashMap<>();

    private CodeClickHandler mCodeHandler;

    private Handler mParseHandler;

    private float[] mLastClickPosition = new float[] { -1, -1};

    public HtmlTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public HtmlTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HtmlTextView(Context context) {
        super(context);
    }

    public void setLinkClickHandler(LinkClickHandler handler) {
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
     * @see HtmlTextView#setHtml(int)
     */
    public void setHtml(@RawRes int resId) {
        setHtml(resId, null);
    }

    /**
     * @see HtmlTextView#setHtml(String)
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
                mDrawables.clear(); // Clear the drawables that were cached for use earlier

                final HtmlTagHandler htmlTagHandler = new HtmlTagHandler(getPaint());
                htmlTagHandler.setClickableTableSpan(clickableTableSpan);
                htmlTagHandler.setDrawTableLinkSpan(drawTableLinkSpan);

                // Override tags to stop Html.fromHtml destroying some of them
                final String overridden = htmlTagHandler.overrideTags(html);

                final Spanned text;
                if(removeFromHtmlSpace) {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        text = removeHtmlBottomPadding(Html.fromHtml(overridden, Html.FROM_HTML_MODE_LEGACY, imageGetter, htmlTagHandler));
                    } else {
                        text = removeHtmlBottomPadding(Html.fromHtml(overridden, imageGetter, htmlTagHandler));
                    }
                } else {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        text = Html.fromHtml(overridden, Html.FROM_HTML_MODE_LEGACY, imageGetter, htmlTagHandler);
                    } else {
                        text = Html.fromHtml(overridden, imageGetter, htmlTagHandler);
                    }
                }

                // Convert to a buffer to allow editing
                final SpannableString buffer = new SpannableString(text);
                //Get the URLSpans that are present before Linkify destroys them
                final URLSpan[] spans = buffer.getSpans(0, buffer.length(), URLSpan.class);

                //Add links for emails and web-urls
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
                //Post back on UI thread
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
        //If we have a handler use it
        if(mParseHandler != null) {
            mParseHandler.postDelayed(r, 20);
        } else {
            r.run();
        }

    }

    @Override
    public void drawableLoaded(Drawable d, String source) {
        mDrawables.put(source, d);
    }

    private void stripUnderLines(Spannable s) {
        final URLSpan[] spans = s.getSpans(0, s.length(), URLSpan.class);
        for(URLSpan span : spans) {
            final int start = s.getSpanStart(span);
            final int end = s.getSpanEnd(span);
            s.removeSpan(span);
            if(mLinkHandler == null) {
                span = new CleanURLSpan(span.getURL());
            } else {
                span = new CleanURLSpan(span.getURL(), mLinkHandler);
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
                        //Get the drawable from our map and call the handler
                        if(mDrawables.containsKey(span.getSource())) {
                            mImageClickHandler.imageClicked(mDrawables.get(span.getSource()));
                        }
                    }
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    // We don't want to show a line below the image
                    ds.setUnderlineText(false);
                }
            }, s.getSpanStart(span), s.getSpanEnd(span), s.getSpanFlags(span));
        }
    }

    private void enableCodeClicks(String text, final Spannable s) {
        // Collect all of the CodeSpans
        final CodeSpan[] spans = s.getSpans(0, s.length(), CodeSpan.class);
        final String[] codes = new String[spans.length];

        int startIndex = 0;
        int endIndex = 0;
        int i = 0; // Index in spans

        while(startIndex != -1 && i < spans.length) {
            // Search for the next code start after our previous position
            startIndex = text.indexOf("<code>", startIndex);
            if(startIndex == -1) break; // No more code tags
            endIndex = text.indexOf("</code>", startIndex);
            
            if(endIndex != -1) { // Ignore tags which aren't closed
                codes[i] = text.substring(startIndex + "<code>".length(), endIndex);
            }
            i++;
            startIndex = endIndex; //Jump to end of current span
        }
        //Set the correct code for each span
        for(CodeSpan span : spans) {
            span.setHandler(mCodeHandler);
            span.setCode(codes[span.getIndex()]);
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

    public float[] getLastClickPosition() {
        if(mLastClickPosition[0] == -1) {
            // If we haven't been clicked yet, get the centre of the view
            final int[] pos = new int[2];
            getLocationOnScreen(pos);
            mLastClickPosition[0] = pos[0] + getWidth() / 2;
            mLastClickPosition[1] = pos[1] + getHeight() / 2;
        }
        return mLastClickPosition;
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
        if(text == null) {
            return null;
        }

        while(text.length() > 0 && text.charAt(text.length() - 1) == '\n') {
            text = (Spanned) text.subSequence(0, text.length() - 1);
        }
        return text;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            mLastClickPosition[0] = event.getRawX();
            mLastClickPosition[1] = event.getRawY();
        }
        linkHit = false;
        boolean res = super.onTouchEvent(event);

        if(dontConsumeNonUrlClicks) {
            return linkHit;
        }
        return res;
    }

}
