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

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.Touch;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;

import com.tpb.mdtext.handlers.NestedScrollHandler;
import com.tpb.mdtext.views.MarkdownEditText;
import com.tpb.mdtext.views.MarkdownTextView;
import com.tpb.mdtext.views.spans.InlineCodeSpan;

/**
 * Copied from http://stackoverflow.com/questions/8558732
 */
public class LocalLinkMovementMethod extends LinkMovementMethod implements GestureDetector.OnGestureListener {
    private GestureDetectorCompat mGestureDetector;
    private NestedScrollHandler mScrollHandler;
    private InlineCodeSpan mLastCodeSpanHit;

    public LocalLinkMovementMethod(Context context, @Nullable NestedScrollHandler nestedScrollHandler) {
        mGestureDetector = new GestureDetectorCompat(context, this);
        mScrollHandler = nestedScrollHandler;
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
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
                    mLastCodeSpanHit = code[0];
                    mScrollHandler.onScrollLocked();
                } else {
                    mScrollHandler.onScrollUnlocked();
                    mLastCodeSpanHit = null;
                }

            }

            if(clickable.length != 0) {
                if(action == MotionEvent.ACTION_UP) {
                    clickable[0].onClick(widget);
                }
                if(widget instanceof MarkdownTextView) {
                    ((MarkdownTextView) widget).setSpanHit();
                } else if(widget instanceof MarkdownEditText) {
                    ((MarkdownEditText) widget).setSpanHit();
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

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        Log.i("Code", "OnScroll " + distanceX + ", " + distanceY);
        if(mLastCodeSpanHit != null) mLastCodeSpanHit.onTouchEvent(e2);
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Log.i("Code", "OnScroll " + velocityX + ", " + velocityY );
        return false;
    }
}
