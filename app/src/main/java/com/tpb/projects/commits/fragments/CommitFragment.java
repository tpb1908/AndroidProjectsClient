package com.tpb.projects.commits.fragments;

import com.tpb.github.data.models.Commit;
import com.tpb.projects.commits.CommitActivity;
import com.tpb.projects.common.ViewSafeFragment;

/**
 * Created by theo on 30/03/17.
 */

public abstract class CommitFragment extends ViewSafeFragment {

    protected Commit mCommit;

    public abstract void commitLoaded(Commit commit);

    public CommitActivity getParent() {
        return (CommitActivity) getActivity();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mAreViewsValid = false;
    }
}
