package com.tpb.projects.repo.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.androidnetworking.widget.ANImageView;
import com.mittsu.markedview.MarkedView;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.Repository;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by theo on 25/03/17.
 */

public class RepoInfoFragment extends RepoFragment {

    private Unbinder unbinder;

    @BindView(R.id.repo_info_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.user_image) ANImageView mAvatar;
    @BindView(R.id.user_name) TextView mUserName;
    @BindView(R.id.repo_collaborators) LinearLayout mCollaborators;
    @BindView(R.id.repo_readme) MarkedView mReadme;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_repo_info, container, false);
        unbinder = ButterKnife.bind(this, view);
        mAreViewsValid = true;
        mRefresher.setRefreshing(true);
        mRefresher.setOnRefreshListener(() -> {
            new Loader(getContext()).loadRepository(new Loader.GITModelLoader<Repository>() {
                @Override
                public void loadComplete(Repository data) {
                    repoLoaded(data);
                }

                @Override
                public void loadError(APIHandler.APIError error) {

                }
            }, mRepository.getFullName());
        });
        return view;
    }

    @Override
    public void repoLoaded(Repository repo) {
        mRefresher.setRefreshing(false);
        mRepository = repo;
        mAvatar.setImageUrl(repo.getUserAvatarUrl());
        mUserName.setText(repo.getUserLogin());

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
