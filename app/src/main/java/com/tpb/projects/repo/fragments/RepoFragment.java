package com.tpb.projects.repo.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.tpb.projects.R;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.repo.RepoActivity;
import com.tpb.projects.util.fab.FloatingActionButton;

/**
 * Created by theo on 25/03/17.
 */

public abstract class RepoFragment extends Fragment {

    protected Repository mRepo;
    protected boolean mAreViewsValid;

    public abstract void repoLoaded(Repository repo);

    public abstract void handleFab(FloatingActionButton fab);

    public abstract void notifyBackPressed();

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(savedInstanceState != null && savedInstanceState.containsKey(getString(R.string.intent_repo))) {
            mRepo = savedInstanceState.getParcelable(getString(R.string.intent_repo));
            repoLoaded(mRepo);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(getString(R.string.intent_repo), mRepo);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mAreViewsValid = false;
    }

    protected RepoActivity getParent() {
        return (RepoActivity) getActivity();
    }

}
