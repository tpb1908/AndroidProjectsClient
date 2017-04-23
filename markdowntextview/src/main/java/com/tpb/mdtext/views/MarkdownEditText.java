package com.tpb.mdtext.views;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.AttributeSet;

import com.tpb.mdtext.ClickableMovementMethod;
import com.tpb.mdtext.HtmlTagHandler;
import com.tpb.mdtext.Markdown;
import com.tpb.mdtext.TextUtils;
import com.tpb.mdtext.dialogs.CodeDialog;
import com.tpb.mdtext.dialogs.ImageDialog;
import com.tpb.mdtext.dialogs.TableDialog;
import com.tpb.mdtext.handlers.CodeClickHandler;
import com.tpb.mdtext.handlers.ImageClickHandler;
import com.tpb.mdtext.handlers.LinkClickHandler;
import com.tpb.mdtext.handlers.TableClickHandler;
import com.tpb.mdtext.views.spans.CodeSpan;
import com.tpb.mdtext.views.spans.TableSpan;

import java.io.InputStream;
import java.util.Scanner;


/**
 * Created by theo on 27/02/17.
 */

public class MarkdownEditText extends AppCompatEditText {

    public static final String TAG = MarkdownEditText.class.getSimpleName();

    private boolean mIsEditing = true;
    private Editable mSavedText = new SpannableStringBuilder();
    @Nullable private LinkClickHandler mLinkHandler;
    @Nullable private ImageClickHandler mImageClickHandler;
    @Nullable private TableClickHandler mTableHandler;
    @Nullable private CodeClickHandler mCodeHandler;


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

    private void init(Context context ) {
        if(!CodeSpan.isInitialised()) CodeSpan.initialise(context);
        if(!TableSpan.isInitialised()) TableSpan.initialise(context);
        setDefaultHandlers(context);
        setPadding(0, getPaddingTop(), 0, getPaddingBottom());
    }

    public void setLinkClickHandler(LinkClickHandler handler) {
        mLinkHandler = handler;
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

    public void setDefaultHandlers(Context context) {
        setCodeClickHandler(new CodeDialog(context));
        setImageHandler(new ImageDialog(context));
        setTableClickHandler(new TableDialog(context));
    }

    public void setMarkdown(@NonNull String html) {
        setMarkdown(html, null);
    }

    public void setMarkdown(@RawRes int resId) {
        setMarkdown(resId, null);
    }

    private void setMarkdown(@RawRes int resId, @Nullable Html.ImageGetter imageGetter) {
        InputStream inputStreamText = getContext().getResources().openRawResource(resId);
        setMarkdown(convertStreamToString(inputStreamText), imageGetter);
    }

    public void setMarkdown(@NonNull final String markdown, @Nullable final Html.ImageGetter imageGetter) {
        // Override tags to stop Html.fromHtml destroying some of them
        final String overridden = HtmlTagHandler.overrideTags(Markdown.parseMD(markdown));
        final HtmlTagHandler htmlTagHandler = new HtmlTagHandler(this,
                imageGetter,  mLinkHandler, mImageClickHandler, mCodeHandler, mTableHandler
        );
        final Spanned text;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            text = removeHtmlBottomPadding(
                    Html.fromHtml(overridden, Html.FROM_HTML_MODE_COMPACT, imageGetter,
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
        setText(buffer);
        if(!(getMovementMethod() instanceof ClickableMovementMethod)) {
            setMovementMethod(ClickableMovementMethod.getInstance());
        }
    }

    @NonNull
    private static String convertStreamToString(@NonNull InputStream is) {
        final Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

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
