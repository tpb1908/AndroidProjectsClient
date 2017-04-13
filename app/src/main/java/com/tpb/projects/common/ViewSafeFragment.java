package com.tpb.projects.common;

import android.support.v4.app.Fragment;

/**
 * Created by theo on 11/04/17.
 */

public class ViewSafeFragment extends Fragment {

    protected boolean mAreViewsValid;

    protected boolean areViewsValid() {
        return mAreViewsValid && getActivity() != null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mAreViewsValid = false;
    }

}
