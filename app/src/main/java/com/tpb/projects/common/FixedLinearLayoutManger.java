package com.tpb.projects.common;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;

/**
 * Created by theo on 31/03/17.
 */

public class FixedLinearLayoutManger extends LinearLayoutManager {

    public FixedLinearLayoutManger(Context context) {
        super(context);
    }


    @Override
    public boolean supportsPredictiveItemAnimations() {
        return false;
    }
}
