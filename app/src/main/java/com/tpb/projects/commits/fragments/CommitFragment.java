package com.tpb.projects.commits.fragments;

import android.support.v4.app.Fragment;

import com.tpb.github.data.models.Commit;
import com.tpb.projects.commits.CommitActivity;

/**
 * Created by theo on 30/03/17.
 */

public abstract class CommitFragment extends Fragment {

    protected boolean mAreViewsValid;
    protected CommitActivity mParent;
    protected Commit mCommit;

    public abstract void commitLoaded(Commit commit);

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mAreViewsValid = false;
    }
}
