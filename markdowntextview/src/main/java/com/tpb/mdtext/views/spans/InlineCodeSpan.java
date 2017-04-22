package com.tpb.mdtext.views.spans;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.text.style.ReplacementSpan;

/**
 * Created by theo on 22/03/17.
 */

public class InlineCodeSpan extends ReplacementSpan {
    private final float mTextSize;

    private float mPadding;


    public InlineCodeSpan(float textSize) {
        mTextSize = textSize;
    }

    @Override
    public void updateDrawState(TextPaint tp) {
        tp.setTextSize(mTextSize);
        tp.setTypeface(Typeface.MONOSPACE);
    }

    @Override
    public int getSize(@NonNull Paint paint,
                       CharSequence text,
                       @IntRange(from = 0) int start,
                       @IntRange(from = 0) int end,
                       @Nullable Paint.FontMetricsInt fm) {
        mPadding = paint.measureText("c");
        return  (int) (paint.measureText(text, start, end) + mPadding * 2);
    }

    @Override
    public void draw(@NonNull Canvas canvas,
                     CharSequence text,
                     @IntRange(from = 0) int start,
                     @IntRange(from = 0) int end,
                     float x,
                     int top,
                     int y,
                     int bottom,
                     @NonNull Paint paint) {
        canvas.drawText(text, start, end, x + mPadding, y, paint);
        paint.setColor(Color.GRAY);
        paint.setAlpha(50);
        final int leading = paint.getFontMetricsInt().leading;
        canvas.drawRect((int) x, top - leading, (int) x + canvas.getWidth(), bottom + leading, paint);
    }


}