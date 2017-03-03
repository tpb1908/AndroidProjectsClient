package com.tpb.animatingrecyclerview;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import jp.wasabeef.recyclerview.animators.FadeInDownAnimator;

/**
 * Created by theo on 17/12/16.
 */

public class AnimatingRecycler extends RecyclerView {

    public AnimatingRecycler(Context context) {
        this(context, null);
        setItemAnimator(new FadeInDownAnimator());
    }

    public AnimatingRecycler(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        setItemAnimator(new FadeInDownAnimator());
    }

    public AnimatingRecycler(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setItemAnimator(new FadeInDownAnimator());
    }

}
