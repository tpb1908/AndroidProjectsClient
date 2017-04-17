package com.tpb.mdtext.views.spans;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.text.style.ReplacementSpan;

/**
 * Created by theo on 02/03/17.
 */

public class HorizontalRuleSpan extends ReplacementSpan {

    private RectF mRectF;

    public HorizontalRuleSpan() {
        mRectF = new RectF();
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return 0;
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
        final int mid = (top + bottom) / 2;
        final int quarter = (bottom - top) / 4;
        paint.setColor(Color.GRAY);

        mRectF.left = x;
        mRectF.top = mid - quarter;
        mRectF.right = x + canvas.getWidth();
        mRectF.bottom = mid + quarter;
        canvas.drawRect(mRectF, paint);
        final int eighth = quarter / 2;
        paint.setColor(Color.LTGRAY);
        mRectF.left += eighth;
        mRectF.right -= eighth;
        mRectF.top += eighth;
        mRectF.bottom -= eighth;
        canvas.drawRect(mRectF, paint);
    }
}
