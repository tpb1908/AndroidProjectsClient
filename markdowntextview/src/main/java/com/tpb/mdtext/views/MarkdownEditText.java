package com.tpb.mdtext.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.view.View;

import com.tpb.mdtext.HtmlTagHandler;
import com.tpb.mdtext.LocalLinkMovementMethod;
import com.tpb.mdtext.Markdown;
import com.tpb.mdtext.TextUtils;
import com.tpb.mdtext.dialogs.CodeDialog;
import com.tpb.mdtext.dialogs.ImageDialog;
import com.tpb.mdtext.dialogs.TableDialog;
import com.tpb.mdtext.handlers.CodeClickHandler;
import com.tpb.mdtext.handlers.ImageClickHandler;
import com.tpb.mdtext.handlers.LinkClickHandler;
import com.tpb.mdtext.handlers.NestedScrollHandler;
import com.tpb.mdtext.handlers.TableClickHandler;
import com.tpb.mdtext.imagegetter.HttpImageGetter;
import com.tpb.mdtext.views.spans.CodeSpan;
import com.tpb.mdtext.views.spans.TableSpan;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Scanner;


/**
 * Created by theo on 27/02/17.
 */

public class MarkdownEditText extends AppCompatEditText implements HttpImageGetter.DrawableCacheHandler{

    public static final String TAG = MarkdownEditText.class.getSimpleName();

    private boolean mIsEditing = true;
    private Editable mSavedText = new SpannableStringBuilder();
    @Nullable private LinkClickHandler mLinkHandler;
    @Nullable private ImageClickHandler mImageClickHandler;
    @Nullable private TableClickHandler mTableHandler;
    private final HashMap<String, Drawable> mDrawables = new HashMap<>();
    @Nullable private CodeClickHandler mCodeHandler;
    @Nullable private Handler mParseHandler;

    private boolean mSpanHit = false;

    public MarkdownEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public MarkdownEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MarkdownEditText(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context ){
        if(!CodeSpan.isInitialised()) CodeSpan.initialise(context);
        if(!TableSpan.isInitialised()) TableSpan.initialise(context);
        setDefaultHandlers(context);
        setPadding(0, getPaddingTop(), 0, getPaddingBottom());
    }

    public void setLinkClickHandler(LinkClickHandler handler) {
        mLinkHandler = handler;
    }

    public void setParseHandler(@Nullable Handler parseHandler) {
        mParseHandler = parseHandler;
    }

    public void setImageHandler(ImageClickHandler imageHandler) {
        mImageClickHandler = imageHandler;
    }

    public void setTableClickHandler(TableClickHandler handler) {
        mTableHandler = handler;
    }

    public void setCodeClickHandler(CodeClickHandler handler) {
        mCodeHandler = handler;
    }

    public void setNestedScrollHandler(NestedScrollHandler handler) {
        setMovementMethod(new LocalLinkMovementMethod(handler));
    }

    public void setDefaultHandlers(Context context) {
        setCodeClickHandler(new CodeDialog(context));
        setImageHandler(new ImageDialog(context));
        setTableClickHandler(new TableDialog(context));
    }

    public void setMarkdown(@RawRes int resId) {
        setMarkdown(resId, null);
    }

    public void setMarkdown(@NonNull String html) {
        setMarkdown(html, null);
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
    private void setMarkdown(@RawRes int resId, @Nullable Html.ImageGetter imageGetter) {
        InputStream inputStreamText = getContext().getResources().openRawResource(resId);
        setMarkdown(convertStreamToString(inputStreamText), imageGetter);
    }

    /**
     * Parses String containing HTML to Android's Spannable format and displays it in this TextView.
     * Using the implementation of Html.ImageGetter provided.
     *
     * @param markdown    String containing HTML, for example: "<b>Hello world!</b>"
     * @param imageGetter for fetching images. Possible ImageGetter provided by this library:
     *                    HtmlLocalImageGetter and HtmlRemoteImageGetter
     */
    public void setMarkdown(@NonNull final String markdown, @Nullable final Html.ImageGetter imageGetter) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                mDrawables.clear(); // Clear the drawables that were cached for use earlier

                final HtmlTagHandler htmlTagHandler = new HtmlTagHandler(MarkdownEditText.this,
                        mLinkHandler, mCodeHandler, mTableHandler
                );

                // Override tags to stop Html.fromHtml destroying some of them
                final String overridden = htmlTagHandler.overrideTags(Markdown.parseMD(markdown));
                final Spanned text;
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    text = removeHtmlBottomPadding(
                            Html.fromHtml(overridden, Html.FROM_HTML_MODE_LEGACY, imageGetter,
                                    htmlTagHandler
                            ));
                } else {
                    text = removeHtmlBottomPadding(
                            Html.fromHtml(overridden, imageGetter, htmlTagHandler));
                }

                // Convert to a buffer to allow editing
                final SpannableString buffer = new SpannableString(text);

                //Add links for emails and web-urls
                TextUtils.addLinks(buffer);

                if(mImageClickHandler != null) {
                    enableImageClicks(buffer);
                }
                //Post back on UI thread
                MarkdownEditText.this.post(new Runnable() {
                    @Override
                    public void run() {
                        setText(buffer);
                        checkMovementMethod();
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

    private void checkMovementMethod() {
        if(!(getMovementMethod() instanceof LocalLinkMovementMethod)) {
            setMovementMethod(new LocalLinkMovementMethod(null));
        }
    }

    @Override
    public void drawableLoaded(Drawable d, String source) {
        mDrawables.put(source, d);
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
    private static Spanned removeHtmlBottomPadding(@Nullable Spanned text) {
        if(text == null) {
            return null;
        }

        while(text.length() > 0 && text.charAt(text.length() - 1) == '\n') {
            text = (Spanned) text.subSequence(0, text.length() - 1);
        }
        return text;
    }

    public void setSpanHit() {
        mSpanHit = true;
    }

    public boolean isEditing() {
        return mIsEditing;
    }

    public void enableEditing() {
        if(mIsEditing) return;
        setFocusable(true);
        setFocusableInTouchMode(true);
        setCursorVisible(true);
        setEnabled(true);
        mIsEditing = true;
    }

    public void disableEditing() {
        if(!mIsEditing) return;
        setBackground(null);
        setFocusable(false);
        setCursorVisible(false);
        mIsEditing = false;
    }

    public void saveText() {
        mSavedText = getText();
    }

    public void restoreText() {
        setText(mSavedText);
    }

    public Editable getInputText() {
        return mIsEditing ? getText() : mSavedText;
    }

    @Override
    public boolean isSuggestionsEnabled() {
        return mIsEditing;
    }

}
