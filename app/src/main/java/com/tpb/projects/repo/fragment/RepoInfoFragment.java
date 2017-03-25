package com.tpb.projects.repo.fragment;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.widget.ANImageView;
import com.mittsu.markedview.MarkedView;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.data.models.User;
import com.tpb.projects.user.UserActivity;
import com.tpb.projects.util.fab.FloatingActionButton;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by theo on 25/03/17.
 */

public class RepoInfoFragment extends RepoFragment {

    private Unbinder unbinder;

    private Loader mLoader;

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
        mLoader = new Loader(getContext());
        mRefresher.setOnRefreshListener(() -> {
            new Loader(getContext()).loadRepository(new Loader.GITModelLoader<Repository>() {
                @Override
                public void loadComplete(Repository data) {
                    repoLoaded(data);
                }

                @Override
                public void loadError(APIHandler.APIError error) {

                }
            }, mRepo.getFullName());
        });
        return view;
    }

    @Override
    public void repoLoaded(Repository repo) {
        mRefresher.setRefreshing(false);
        mRepo = repo;
        mAvatar.setImageUrl(repo.getUserAvatarUrl());
        mUserName.setText(repo.getUserLogin());

        mLoader.loadReadMe(new Loader.GITModelLoader<String>() {
            @Override
            public void loadComplete(String data) {
                mReadme.setVisibility(View.VISIBLE);
                mReadme.setMDText(data);
                mReadme.reload();
            }

            @Override
            public void loadError(APIHandler.APIError error) {
                if(error == APIHandler.APIError.NOT_FOUND) {
                    Toast.makeText(getContext(), R.string.error_readme_not_found, Toast.LENGTH_SHORT).show();
                }
            }
        }, mRepo.getFullName());
        
        mLoader.loadCollaborators(new Loader.GITModelsLoader<User>() {
            @Override
            public void loadComplete(User[] collaborators) {
                displayCollaborators(collaborators);
            }

            @Override
            public void loadError(APIHandler.APIError error) {

            }
        }, mRepo.getFullName());
    }

    @Override
    public void handleFab(FloatingActionButton fab) {
        fab.hide(true);
    }

    private void displayCollaborators(User[] collaborators) {
        mCollaborators.removeAllViews();
        if(collaborators.length > 1) {
            mCollaborators.setVisibility(View.VISIBLE);
            for(int i = 0; i < collaborators.length; i++) {
                final User u = collaborators[i];
                final LinearLayout user = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.shard_user, null);
                user.setId(i);
                mCollaborators.addView(user);
                final ANImageView imageView = (ANImageView) user.findViewById(R.id.user_image);
                imageView.setId(View.generateViewId());
                imageView.setImageUrl(u.getAvatarUrl());
                imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                final TextView login = (TextView) user.findViewById(R.id.user_login);
                login.setId(View.generateViewId());
                login.setText(u.getLogin());
                user.setOnClickListener((v) -> {
                    final Intent us = new Intent(getActivity(), UserActivity.class);
                    us.putExtra(getString(R.string.intent_username), u.getLogin());

                    if(imageView.getDrawable() != null) {
                        us.putExtra(getString(R.string.intent_drawable), ((BitmapDrawable) imageView.getDrawable()).getBitmap());
                    }
                    getActivity().startActivity(us,
                            ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    getActivity(),
                                    Pair.create(login, getString(R.string.transition_username)),
                                    Pair.create(imageView, getString(R.string.transition_user_image))
                            ).toBundle());
                });
            }
        } else {
            mCollaborators.setVisibility(View.GONE);
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
