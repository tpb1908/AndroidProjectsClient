package com.tpb.projects.util.input;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

/**
 * Created by theo on 21/02/17.
 */

public class KeyBoardVisibilityChecker {

    private boolean mIsKeyboardOpen = false;

    public KeyBoardVisibilityChecker(@NonNull View content) {
        this(content, null);
    }

    public KeyBoardVisibilityChecker(@NonNull View content, @Nullable KeyBoardVisibilityListener listener) {
        content.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            final Rect r = new Rect();
            content.getWindowVisibleDisplayFrame(r);
            final int screenHeight = content.getRootView().getHeight();

            // r.bottom is the position above soft keypad or device button.
            // if keypad is shown, the r.bottom is smaller than that before.
            final int keypadHeight = screenHeight - r.bottom;

            if(keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
                mIsKeyboardOpen = true;
                if(listener != null) listener.keyboardShown();
                // keyboard is opened
            } else {
                mIsKeyboardOpen = false;
                if(listener != null) listener.keyboardHidden();
            }
        });
    }

    public boolean isKeyboardOpen() {
        return mIsKeyboardOpen;
    }

    public interface KeyBoardVisibilityListener {

        void keyboardShown();

        void keyboardHidden();

    }

}
