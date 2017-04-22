package com.tpb.mdtext.views.spans;

import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

import com.tpb.mdtext.handlers.ImageClickHandler;
import com.tpb.mdtext.imagegetter.HttpImageGetter;

import java.lang.ref.WeakReference;

/**
 * Created by theo on 22/04/17.
 */

public class ClickableImageSpan extends ImageSpan implements WrappingClickableSpan.WrappedClickableSpan {

    private WeakReference<ImageClickHandler> mImageClickHandler;

    public ClickableImageSpan(Drawable d, ImageClickHandler handler) {
        super(d);
        mImageClickHandler = new WeakReference<>(handler);
    }

    @Override
    public Drawable getDrawable() {
        if(super.getDrawable() instanceof HttpImageGetter.URLDrawable && ((HttpImageGetter.URLDrawable) super.getDrawable()).getDrawable() != null) {
            return ((HttpImageGetter.URLDrawable) super.getDrawable()).getDrawable();
        }
        return super.getDrawable();
    }

    @Override
    public void onClick() {
        if(mImageClickHandler.get() != null) {
            mImageClickHandler.get().imageClicked(getDrawable());
        }
    }
}
