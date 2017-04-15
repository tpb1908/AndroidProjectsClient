/*
 * Copyright (C) 2015 Heliangwei
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tpb.mdtext;

import android.support.annotation.Nullable;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.Touch;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.widget.TextView;

import com.tpb.mdtext.handlers.NestedScrollHandler;
import com.tpb.mdtext.views.MarkdownEditText;
import com.tpb.mdtext.views.MarkdownTextView;
import com.tpb.mdtext.views.spans.InlineCodeSpan;

/**
 * Copied from http://stackoverflow.com/questions/8558732
 */
public class LocalLinkMovementMethod extends LinkMovementMethod {
    private NestedScrollHandler mScrollHandler;

    public LocalLinkMovementMethod(@Nullable NestedScrollHandler nestedScrollHandler) {
        mScrollHandler = nestedScrollHandler;
    }

    @Override
    public boolean onTouchEvent(final TextView widget, final Spannable buffer, MotionEvent event) {
        final int action = event.getAction();
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

        if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {

            if(mScrollHandler != null) {
                final InlineCodeSpan[] code = buffer.getSpans(off, off, InlineCodeSpan.class);
                if(code.length > 0 && action == MotionEvent.ACTION_DOWN) {
                    code[0].onTouchEvent(x > layout.getWidth() /2);

                    widget.post(new Runnable() {
                        @Override
                        public void run() {
                            Selection.removeSelection(buffer);
                        }
                    });
                    mScrollHandler.onScrollLocked();
                } else {
                    mScrollHandler.onScrollUnlocked();

                }
                return true;
            }

            if(clickable.length != 0) {
                if(action == MotionEvent.ACTION_UP) {
                    clickable[0].onClick(widget);
                }
                triggerSpanHit(widget);
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
