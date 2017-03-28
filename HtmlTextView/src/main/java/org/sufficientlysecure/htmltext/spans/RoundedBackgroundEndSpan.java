package org.sufficientlysecure.htmltext.spans;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.text.style.ReplacementSpan;

/**
 * Created by theo on 28/03/17.
 */

public class RoundedBackgroundEndSpan extends ReplacementSpan {

    private int mCharacterWidth = 0;
    private final int mBgColor;
    private final boolean mIsEndSpan;
    private RectF mRectF;

    public RoundedBackgroundEndSpan(int bgColor, boolean isEndSpan) {
        mBgColor = bgColor;
        mIsEndSpan = isEndSpan;
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        mCharacterWidth = (int) paint.measureText("tt");
        return mCharacterWidth;
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
        mRectF = new RectF(x, top, x + mCharacterWidth, bottom);
        paint.setColor(mBgColor);
        canvas.drawRoundRect(mRectF, (bottom-top), (bottom-top), paint);
        if(mIsEndSpan) {
            mRectF = new RectF(x , top, (x + x + mCharacterWidth)/2, bottom);
        } else {
            mRectF = new RectF((x + x + mCharacterWidth)/2, top, x + mCharacterWidth, bottom);
        }
        canvas.drawRect(mRectF, paint);
    }
}
