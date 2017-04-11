package com.tpb.mdtext.views.spans;

import android.support.annotation.NonNull;
import android.text.style.ClickableSpan;
import android.view.View;

/**
 * Created by theo on 11/04/17.
 */

public class WrappingClickableSpan extends ClickableSpan {

    private WrappedClickableSpan mWrappedClickableSpan;

    public WrappingClickableSpan(@NonNull WrappedClickableSpan child) {
        mWrappedClickableSpan = child;
    }

    @Override
    public void onClick(View widget) {
        mWrappedClickableSpan.onClick();
    }

    public interface WrappedClickableSpan {

        void onClick();

    }

}
