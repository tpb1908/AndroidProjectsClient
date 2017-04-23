package com.tpb.mdtext;

import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.Touch;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.widget.TextView;

import com.tpb.mdtext.views.MarkdownEditText;
import com.tpb.mdtext.views.MarkdownTextView;

/**
 * Copied from http://stackoverflow.com/questions/8558732
 */
public class ClickableMovementMethod extends LinkMovementMethod {

    private static ClickableMovementMethod instance;

    private ClickableMovementMethod() {}

    public static ClickableMovementMethod getInstance() {
        if(instance == null) instance = new ClickableMovementMethod();
        return instance;
    }

    @Override
    public boolean onTouchEvent(final TextView widget, final Spannable buffer, MotionEvent event) {
        final int action = event.getAction();

        if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {

            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            x += widget.getScrollX();
            y += widget.getScrollY();

            final Layout layout = widget.getLayout();
            final int line = layout.getLineForVertical(y);
            final int off = layout.getOffsetForHorizontal(line, x);

            final ClickableSpan[] clickable = buffer.getSpans(off, off, ClickableSpan.class);

            if(clickable.length != 0) {
                if(action == MotionEvent.ACTION_UP) {
                    clickable[0].onClick(widget);
                    triggerSpanHit(widget);
                }

                return true;
            } else {
                Selection.removeSelection(buffer);
                Touch.onTouchEvent(widget, buffer, event);
                return false;
            }
        }

        return Touch.onTouchEvent(widget, buffer, event);
    }

    private void triggerSpanHit(TextView widget) {
        if(widget instanceof MarkdownTextView) {
            ((MarkdownTextView) widget).setSpanHit();
        } else if(widget instanceof MarkdownEditText) {
            ((MarkdownEditText) widget).setSpanHit();
        }
    }

}
