package com.tpb.mdtext.views;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.v7.widget.AppCompatTextView;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.tpb.mdtext.ClickableMovementMethod;
import com.tpb.mdtext.HtmlTagHandler;
import com.tpb.mdtext.Markdown;
import com.tpb.mdtext.SpanCache;
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
import java.lang.ref.WeakReference;
import java.util.Scanner;


public class MarkdownTextView extends AppCompatTextView implements View.OnClickListener {

    public static final String TAG = MarkdownTextView.class.getSimpleName();

    @Nullable private LinkClickHandler mLinkHandler;
    @Nullable private ImageClickHandler mImageClickHandler;
    @Nullable private TableClickHandler mTableHandler;
    @Nullable private CodeClickHandler mCodeHandler;
    @Nullable private Handler mParseHandler;

    private boolean mSpanHit = false;
    private OnClickListener mOnClickListener;
    private float[] mLastClickPosition = new float[] {-1, -1};

    private WeakReference<SpanCache> mSpanCache;

    public MarkdownTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public MarkdownTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MarkdownTextView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        if(!CodeSpan.isInitialised()) CodeSpan.initialise(context);
        if(!TableSpan.isInitialised()) TableSpan.initialise(context);
        setDefaultHandlers(context);
        setTextIsSelectable(true);
        setCursorVisible(false);
        setClickable(true);
        setTextColor(Color.WHITE);
    }

    public void setParseHandler(@Nullable Handler parseHandler) {
        mParseHandler = parseHandler;
    }

    public void setLinkClickHandler(LinkClickHandler handler) {
        mLinkHandler = handler;
    }

    public void setImageHandler(ImageClickHandler imageHandler) {
        mImageClickHandler = imageHandler;
    }

    public void setCodeClickHandler(CodeClickHandler handler) {
        mCodeHandler = handler;
    }

    public void setTableClickHandler(TableClickHandler handler) {
        mTableHandler = handler;
    }

    public void setDefaultHandlers(Context context) {
        setCodeClickHandler(new CodeDialog(context));
        setImageHandler(new ImageDialog(context));
        setTableClickHandler(new TableDialog(context));
    }

    public void setMarkdown(@NonNull String markdown) {
        setMarkdown(markdown, null, null);
    }

    public void setMarkdown(@RawRes int resId) {
        setMarkdown(resId, null);
    }

    private void setMarkdown(@RawRes int resId, @Nullable Html.ImageGetter imageGetter) {
        final InputStream inputStreamText = getContext().getResources().openRawResource(resId);
        setMarkdown(convertStreamToString(inputStreamText), imageGetter, null);
    }

    public void setMarkdown(@NonNull final String markdown, @Nullable final Html.ImageGetter imageGetter, @Nullable SpanCache cache) {
        mSpanCache = new WeakReference<>(cache);
        //If we have a handler use it
        if(mParseHandler != null) {
            mParseHandler.post(new Runnable() {
                @Override
                public void run() {
                    parseAndSetMd(markdown, imageGetter);
                }
            });
        } else {
            parseAndSetMd(markdown, imageGetter);
        }
    }

    private void parseAndSetMd(@NonNull String markdown, @Nullable final Html.ImageGetter imageGetter) {
        // Override tags to stop Html.fromHtml destroying some of them
        markdown = HtmlTagHandler.overrideTags(Markdown.parseMD(markdown));
        final HtmlTagHandler htmlTagHandler = new HtmlTagHandler(this,
                imageGetter,  mLinkHandler, mImageClickHandler, mCodeHandler, mTableHandler
        );
        final Spanned text;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            text = removeHtmlBottomPadding(
                    Html.fromHtml(markdown, Html.FROM_HTML_MODE_COMPACT, imageGetter,
                            htmlTagHandler
                    ));
        } else {
            text = removeHtmlBottomPadding(Html.fromHtml(markdown, imageGetter, htmlTagHandler));
        }

        // Convert to a buffer to allow editing
        final SpannableString buffer = new SpannableString(text);

        //Add links for emails and web-urls
        TextUtils.addLinks(buffer);
        if(Looper.myLooper() == Looper.getMainLooper()) {
            setMarkdownText(buffer);
        } else {
            //Post back on UI thread
            MarkdownTextView.this.post(new Runnable() {
                @Override
                public void run() {
                    setMarkdownText(buffer);
                }
            });
        }
    }

    private void setMarkdownText(SpannableString buffer) {
        setText(buffer);
        if(!(getMovementMethod() instanceof ClickableMovementMethod)) {
            setMovementMethod(ClickableMovementMethod.getInstance());
        }
        if(mSpanCache != null && mSpanCache.get() != null) {
            mSpanCache.get().cache(buffer);
            mSpanCache = null;
        }
    }

    @NonNull
    private static String convertStreamToString(@NonNull InputStream is) {
        final Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private static Spanned removeHtmlBottomPadding(Spanned text) {
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
            if(hasSelection()) clearFocus();
        } else if(event.getAction() == MotionEvent.ACTION_UP) {
            mSpanHit = false;
        }
        return super.onTouchEvent(event);
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

    public void setSpanHit() {
        mSpanHit = true;
    }

    @Override
    public void onClick(View v) {
        if(!mSpanHit && mOnClickListener != null) mOnClickListener.onClick(v);
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        mOnClickListener = l;
        super.setOnClickListener(this);
    }

    @Override
    protected boolean getDefaultEditable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N || super.getDefaultEditable();
    }

    @Override
    public boolean isSuggestionsEnabled() {
        return false;
    }
}
