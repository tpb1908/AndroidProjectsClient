package com.tpb.mdtext.views.spans;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.text.style.ReplacementSpan;
import android.util.Log;
import android.widget.TextView;

import static android.R.attr.padding;

/**
 * Created by theo on 22/03/17.
 */

public class InlineCodeSpan extends ReplacementSpan {
    private final float mTextSize;

    private GradientDrawable mDrawable;
    private float mPadding;
    private int mWidth;
    private int offset = 0;

    public InlineCodeSpan(float textSize) {
        mTextSize = textSize;
        mDrawable = new GradientDrawable();
        mDrawable.setColor(Color.GRAY);
        mDrawable.setAlpha(50);
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
        mWidth = (int) (paint.measureText(text, start, end) + padding * 2);
        return mWidth;
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
        final int leading = paint.getFontMetricsInt().leading;
        mDrawable.setBounds((int) x, top - leading, (int) x + mWidth, bottom + leading);
        mDrawable.draw(canvas);
        Log.i("Code", "Drawing");
        start = Math.max(Math.min(start + offset, end), start);
        canvas.drawText(text, start, end, x + mPadding, y, paint);
    }

    public void onTouchEvent(TextView parent, float distanceX) {
        Log.i("Code", "Touch event ");
        offset += Math.signum(distanceX);
        parent.postInvalidate();
    }


}