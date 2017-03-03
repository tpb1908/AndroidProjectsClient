package org.sufficientlysecure.htmltext;

import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import org.sufficientlysecure.htmltext.handlers.CodeClickHandler;

/**
 * Created by theo on 27/02/17.
 */

public class CodeSpan extends ClickableSpan {
    private CodeClickHandler mHandler;
    private String mCode;
    private int mIndex;

    public CodeSpan(int index) {
        this.mIndex = index;
    }

    public void setHandler(CodeClickHandler handler) {
        mHandler = handler;
    }

    public void setCode(String code) {
        mCode = code;
    }

    public int getIndex() {
        return mIndex;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        super.updateDrawState(ds);
        ds.setUnderlineText(false);
        ds.setTypeface(Typeface.DEFAULT_BOLD);
    }

    @Override
    public void onClick(View widget) {
        if(mHandler != null) {
            mHandler.codeClicked(mCode);
        }
    }
}
