package com.tpb.projects.repo.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.tpb.github.data.models.Repository;
import com.tpb.projects.R;
import com.tpb.projects.common.ViewSafeFragment;
import com.tpb.projects.common.fab.FloatingActionButton;
import com.tpb.projects.repo.RepoActivity;

/**
 * Created by theo on 25/03/17.
 */

public abstract class RepoFragment extends ViewSafeFragment {

    protected Repository mRepo;

    public abstract void repoLoaded(Repository repo);

    public abstract void handleFab(FloatingActionButton fab);

    public abstract void notifyBackPressed();

    public boolean areViewsValid() {
        return mAreViewsValid;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(savedInstanceState != null && savedInstanceState
                .containsKey(getString(R.string.intent_repo))) {
            mRepo = savedInstanceState.getParcelable(getString(R.string.intent_repo));
            repoLoaded(mRepo);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(getString(R.string.intent_repo), mRepo);
    }

    protected RepoActivity getParent() {
        return (RepoActivity) getActivity();
    }

}
