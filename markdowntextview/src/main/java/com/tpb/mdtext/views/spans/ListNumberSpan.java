package com.tpb.mdtext.views.spans;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.LeadingMarginSpan;

import com.tpb.mdtext.TextUtils;

import java.util.TreeMap;

/**
 * Created by theo on 02/03/17.
 */

public class ListNumberSpan implements LeadingMarginSpan {
    private final String mNumber;
    private final int mTextWidth;

    public ListNumberSpan(TextPaint textPaint, int number, ListType type) {
        mNumber = ListType.getFormattedNumber(number + type.start, type).concat(". ");
        mTextWidth = (int) textPaint.measureText(mNumber);
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return mTextWidth;
    }

    @Override
    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline,
                                  int bottom, CharSequence text, int start, int end,
                                  boolean first, Layout l) {
        //Check if we are at the correct depth to draw text rather than just spacing
        if(text instanceof Spanned) {
            if(((Spanned) text).getSpanStart(this) == start) {
                c.drawText(mNumber, x, baseline, p);
            }
        }
    }

    public enum ListType {

        NUMBER,
        LETTER,
        LETTER_CAP,
        ROMAN,
        ROMAN_CAP;

        int start = 0;

        public static ListType fromString(@NonNull String val) {
            if(val.isEmpty()) return NUMBER;
            if(TextUtils.isInteger(val)) {
                final ListType num = NUMBER;
                num.start = Integer.parseInt(val) - 1;
                return num;
            } else {
                switch(val.charAt(0)) {
                    case 'a': return LETTER;
                    case 'A': return LETTER_CAP;
                    case 'i': return ROMAN;
                    case 'I': return ROMAN_CAP;
                    default: return NUMBER;
                }
            }
        }

        public static String getFormattedNumber(int num, ListType type) {
            switch(type) {
                case LETTER:
                    return getLetter(num);
                case LETTER_CAP:
                    return getLetter(num).toUpperCase();
                case ROMAN:
                    return getRoman(num);
                case ROMAN_CAP:
                    return getRoman(num).toUpperCase();
                default:
                    return Integer.toString(num);
            }
        }

        private static String getLetter(int num) {
            final StringBuilder builder = new StringBuilder();
            while(num-- > 0) { //1 = a, not 0 = a
                final int rmdr = num % 26;
                builder.append((char) (rmdr + 'a'));
                num = (num - rmdr) / 26;
            }
            return builder.reverse().toString();
        }

        private static TreeMap<Integer, String> map = new TreeMap<>();

        static {
            map.put(1000, "m");
            map.put(900, "cm");
            map.put(500, "d");
            map.put(400, "cd");
            map.put(100, "c");
            map.put(90, "xc");
            map.put(50, "l");
            map.put(40, "xl");
            map.put(10, "x");
            map.put(9, "xi");
            map.put(5, "v");
            map.put(4, "iv");
            map.put(1, "i");
        }

        private static String getRoman(int num) {
            final int l = map.floorKey(num);
            if(l == num) {
                return map.get(num);
            }
            return map.get(l) + getRoman(num - l);
        }

    }

}
