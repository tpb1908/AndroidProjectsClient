package com.tpb.animatingrecyclerview;

import android.content.Context;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import jp.wasabeef.recyclerview.animators.FadeInDownAnimator;

/**
 * Created by theo on 17/12/16.
 */

public class AnimatingRecyclerView extends RecyclerView {

    private ItemDecoration mItemDecoration;

    public AnimatingRecyclerView(Context context) {
        super(context);
        setItemAnimator(new FadeInDownAnimator());
    }

    public AnimatingRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        setItemAnimator(new FadeInDownAnimator());
    }

    public AnimatingRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setItemAnimator(new FadeInDownAnimator());
    }

    public void enableLineDecoration() {
        disableItemDecoration();
        mItemDecoration = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        addItemDecoration(mItemDecoration);
    }

    public void disableItemDecoration() {
        removeItemDecoration(mItemDecoration);
    }

}
