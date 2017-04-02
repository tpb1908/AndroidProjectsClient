package com.tpb.mdtext.views.spans;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Layout;
import android.text.style.LeadingMarginSpan;

/**
 * Created by theo on 20/03/17.
 */

public class QuoteSpan implements LeadingMarginSpan {
    private static final int STRIPE_WIDTH = 4;
    private static final int GAP_WIDTH = 8;
    private final int mColor;

    public QuoteSpan() {
        super();
        mColor = Color.WHITE;
    }

    public QuoteSpan(int color) {
        super();
        mColor = color;
    }

    public int describeContents() {
        return 0;
    }

    public int getLeadingMargin(boolean first) {
        return STRIPE_WIDTH + GAP_WIDTH;
    }

    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir,
                                  int top, int baseline, int bottom,
                                  CharSequence text, int start, int end,
                                  boolean first, Layout layout) {
        Paint.Style style = p.getStyle();
        final int color = p.getColor();
        p.setStyle(Paint.Style.FILL);
        p.setColor(mColor);
        c.drawRect(x, top, x + dir * STRIPE_WIDTH, bottom, p);
        p.setStyle(style);
        p.setColor(color);
    }
}