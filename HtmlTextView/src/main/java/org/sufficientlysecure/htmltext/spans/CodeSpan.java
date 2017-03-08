package org.sufficientlysecure.htmltext.spans;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.style.ClickableSpan;
import android.text.style.ReplacementSpan;
import android.view.View;

import org.sufficientlysecure.htmltext.handlers.CodeClickHandler;

import java.lang.ref.WeakReference;

/**
 * Created by theo on 06/03/17.
 */

public class CodeSpan extends ReplacementSpan {
    private WeakReference<CodeClickHandler> mHandler;
    private String mCode;
    private String mLanguage;
    private int mIndex;

    public CodeSpan(int index) {
        this.mIndex = index;
    }

    public void setHandler(CodeClickHandler handler) {
        mHandler = new WeakReference<>(handler);
    }

    public void setCode(String code) {
        final int ls = code.indexOf('[');
        final int le = code.indexOf(']');
        if(ls != -1 && le != -1 && le - ls > 0 && le < code.indexOf('\u0002')) {
            mLanguage = code.substring(ls+1, le);
            mCode = code.substring(le + 1);
        } else {
            mCode = code;
        }
    }

    public int getIndex() {
        return mIndex;
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, @IntRange(from = 0) int start, @IntRange(from = 0) int end, @Nullable Paint.FontMetricsInt fm) {
        return 0;
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, @IntRange(from = 0) int start, @IntRange(from = 0) int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
        paint.setTextSize(paint.getTextSize()-1);
        if(mLanguage != null && !mLanguage.isEmpty()) {
            canvas.drawText(mLanguage + " code", x + 5, y + (top-bottom) / 2, paint);
        } else {
            canvas.drawText("Code", x + 5, y + (top-bottom) / 2, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        canvas.drawRoundRect(new RectF(x, top + top - bottom, x + canvas.getWidth(), bottom), 7, 7, paint);
    }

    private void onClick() {
        if(mHandler.get() != null) mHandler.get().codeClicked(mCode, mLanguage);
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
    }

}
