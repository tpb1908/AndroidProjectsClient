package com.tpb.mdtext.views.spans;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ReplacementSpan;
import android.view.View;

import com.tpb.mdtext.handlers.CodeClickHandler;
import org.sufficientlysecure.htmltextview.R;

import java.lang.ref.WeakReference;

/**
 * Created by theo on 06/03/17.
 */

public class CodeSpan extends ReplacementSpan {
    private WeakReference<CodeClickHandler> mHandler;
    private String mCode;
    private String mLanguage;
    private static String mLanguageFormatString = "%1$s code";
    private static String mNoLanguageString = "Code";
    private static Bitmap mCodeBM;
    private PorterDuffColorFilter mBMFilter;

    public CodeSpan(String code, CodeClickHandler handler) {
        setCode(code);
        mHandler = new WeakReference<>(handler);
    }

    public void setCode(String code) {
        final int ls = code.indexOf('[');
        final int le = code.indexOf(']');
        if(ls != -1 && le != -1 && le - ls > 0 && le < code.indexOf("\n")) {
            mLanguage = code.substring(ls + 1, le);
            mCode = code.substring(le + 1);
        } else {
            mCode = code;
        }
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, @IntRange(from = 0) int start, @IntRange(from = 0) int end, @Nullable Paint.FontMetricsInt fm) {
        return 0;
    }


    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, @IntRange(from = 0) int start, @IntRange(from = 0) int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
        paint.setTextSize(paint.getTextSize() - 1);
        final int textHeight = paint.getFontMetricsInt().descent - paint.getFontMetricsInt().ascent;

        int offset = 5;
        if(mCodeBM != null) offset += mCodeBM.getWidth();

        final int textStart = top + textHeight / 4;

        if(mLanguage != null && !mLanguage.isEmpty()) {
            mLanguage = mLanguage.substring(0, 1).toUpperCase() + mLanguage.substring(1);
            canvas.drawText(String.format(mLanguageFormatString, mLanguage), x + offset, textStart,
                    paint
            );
        } else {
            canvas.drawText(mNoLanguageString, x + offset, textStart, paint);
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        canvas.drawRoundRect(new RectF(x, top + top - bottom, x + canvas.getWidth(), bottom), 7, 7,
                paint
        );

        if(mCodeBM != null) {
            if(mBMFilter == null)
                mBMFilter = new PorterDuffColorFilter(paint.getColor(), PorterDuff.Mode.SRC_IN);
            paint.setColorFilter(mBMFilter);
            canvas.drawBitmap(mCodeBM, x, textStart - textHeight, paint);
        }
    }

    private void onClick() {
        if(mHandler.get() != null) mHandler.get().codeClicked(mCode, mLanguage);
    }

    public static void initialise(Context context) {
        final Drawable drawable = context.getResources().getDrawable(R.drawable.ic_code);
        final Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888
        );
        final Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        mCodeBM = bitmap;
        mLanguageFormatString = context.getString(R.string.code_span_language_format);
        mNoLanguageString = context.getString(R.string.code_span_no_language);
    }

    public static boolean isInitialised() {
        return mCodeBM != null;
    }

    public static class ClickableCodeSpan extends ClickableSpan {

        private final CodeSpan mParent;

        public ClickableCodeSpan(CodeSpan parent) {
            mParent = parent;
        }

        @Override
        public void onClick(View widget) {
            mParent.onClick();
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
        }
    }

}
