package org.sufficientlysecure.htmltext;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.text.style.ReplacementSpan;

/**
 * Created by theo on 02/03/17.
 */

public class BarSpan extends ReplacementSpan {

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return 0;
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
        final int mid = (top + bottom) / 2;
        final RectF r = new RectF(x, mid, x + canvas.getWidth(), mid + 3);
        canvas.drawRect(r, paint);
    }
}
