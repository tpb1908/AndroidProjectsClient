package com.tpb.projects.util.fab;

import android.support.v7.widget.RecyclerView;

/**
 * Created by theo on 25/03/17.
 */

public class FabHideScrollListener extends RecyclerView.OnScrollListener {

    private FloatingActionButton mFab;

    public FabHideScrollListener(FloatingActionButton fab) {
        mFab = fab;
    }

    public void setFab(FloatingActionButton fab) {
        mFab = fab;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        if(mFab == null) return;
        if(dy > 10) {
            mFab.hide(true);
        } else if(dy < -10){
            mFab.show(true);
        }
    }
}
