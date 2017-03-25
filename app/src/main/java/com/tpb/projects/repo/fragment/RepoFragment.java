package com.tpb.projects.repo.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.tpb.projects.R;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.repo.RepoActivityNew;

/**
 * Created by theo on 25/03/17.
 */

public abstract class RepoFragment extends Fragment {

    protected Repository mRepository;
    protected boolean mAreViewsValid;

    public abstract void repoLoaded(Repository repo);

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(savedInstanceState != null && savedInstanceState.containsKey(getString(R.string.intent_repo))) {
            mRepository = savedInstanceState.getParcelable(getString(R.string.intent_repo));
            repoLoaded(mRepository);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(getString(R.string.intent_repo), mRepository);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mAreViewsValid = false;
    }

    protected RepoActivityNew getParent() {
        return (RepoActivityNew) getActivity();
    }

}
