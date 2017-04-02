package com.tpb.projects.repo.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
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

import com.tpb.github.data.APIHandler;
import com.tpb.github.data.Loader;
import com.tpb.github.data.models.Repository;
import com.tpb.github.data.models.User;
import com.tpb.projects.R;
import com.tpb.projects.repo.RepoActivity;
import com.tpb.projects.repo.content.ContentActivity;
import com.tpb.projects.user.UserActivity;
import com.tpb.projects.common.NetworkImageView;
import com.tpb.projects.util.Util;
import com.tpb.projects.common.fab.FloatingActionButton;

import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Created by theo on 25/03/17.
 */

public class RepoInfoFragment extends RepoFragment {

    private Unbinder unbinder;

    private Loader mLoader;

    @BindView(R.id.repo_info_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.user_avatar) NetworkImageView mAvatar;
    @BindView(R.id.user_name) TextView mUserName;
    @BindView(R.id.repo_collaborators) LinearLayout mCollaborators;
    @BindView(R.id.repo_contributors) LinearLayout mContributors;

    @BindView(R.id.repo_size) TextView mSize;
    @BindView(R.id.repo_stars) TextView mStars;
    @BindView(R.id.repo_issues) TextView mIssues;
    @BindView(R.id.repo_forks) TextView mForks;
    @BindView(R.id.repo_license) TextView mLicense;

    public static RepoInfoFragment newInstance(RepoActivity parent) {
        final RepoInfoFragment rif = new RepoInfoFragment();
        rif.mParent = parent;
        return rif;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_repo_info, container, false);
        unbinder = ButterKnife.bind(this, view);
        mAreViewsValid = true;
        mRefresher.setRefreshing(true);
        mLoader = new Loader(getContext());
        mRefresher.setOnRefreshListener(() -> {
            new Loader(getContext()).loadRepository(new Loader.ItemLoader<Repository>() {
                @Override
                public void loadComplete(Repository data) {
                    repoLoaded(data);
                }

                @Override
                public void loadError(APIHandler.APIError error) {

                }
            }, mRepo.getFullName());
        });
        if(mRepo != null) repoLoaded(mRepo);
        mParent.notifyFragmentViewCreated(this);
        return view;
    }

    @Override
    public void repoLoaded(Repository repo) {
        mRepo = repo;
        if(!mAreViewsValid) return;
        mRefresher.setRefreshing(false);
        mAvatar.setImageUrl(repo.getUserAvatarUrl());
        mUserName.setText(repo.getUserLogin());
        mIssues.setText(String.valueOf(repo.getIssues()));
        mForks.setText(String.valueOf(repo.getForks()));
        mSize.setText(Util.formatKB(repo.getSize()));
        mStars.setText(String.valueOf(repo.getStarGazers()));
        if(mRepo.hasLicense()) {
            mLicense.setText(repo.getLicenseShortName());
        } else {
            mLicense.setText(R.string.text_no_license);
        }
        loadRelevantUsers();
    }

    @Override
    public void handleFab(FloatingActionButton fab) {
        fab.hide(true);
    }

    private void loadRelevantUsers() {
        mLoader.loadCollaborators(new Loader.ListLoader<User>() {
            @Override
            public void listLoadComplete(List<User> collaborators) {
                displayCollaborators(collaborators);
            }

            @Override
            public void listLoadError(APIHandler.APIError error) {
                mCollaborators.setVisibility(View.GONE);
                ButterKnife.findById(getActivity(), R.id.repo_collaborators_text)
                           .setVisibility(View.GONE);
            }
        }, mRepo.getFullName());
        mLoader.loadContributors(new Loader.ListLoader<User>() {
            @Override
            public void listLoadComplete(List<User> contributors) {
                displayContributors(contributors);
            }

            @Override
            public void listLoadError(APIHandler.APIError error) {
                mContributors.setVisibility(View.GONE);
                ButterKnife.findById(getActivity(), R.id.repo_contributors_text)
                           .setVisibility(View.GONE);
            }
        }, mRepo.getFullName());
    }

    private void displayCollaborators(List<User> collaborators) {
        mCollaborators.removeAllViews();
        if(collaborators.size() > 1) {
            mCollaborators.setVisibility(View.VISIBLE);
            ButterKnife.findById(getActivity(), R.id.repo_collaborators_text)
                       .setVisibility(View.VISIBLE);
            for(final User u : collaborators) mCollaborators.addView(getUserView(u));
        } else {
            mCollaborators.setVisibility(View.GONE);
            ButterKnife.findById(getActivity(), R.id.repo_collaborators_text)
                       .setVisibility(View.GONE);
        }
    }

    private void displayContributors(List<User> contributors) {
        mContributors.removeAllViews();
        if(contributors.size() > 1) {
            mContributors.setVisibility(View.VISIBLE);
            ButterKnife.findById(getActivity(), R.id.repo_contributors_text)
                       .setVisibility(View.VISIBLE);
            for(final User u : contributors) mContributors.addView(getUserView(u));
        } else {
            mContributors.setVisibility(View.GONE);
            ButterKnife.findById(getActivity(), R.id.repo_contributors_text)
                       .setVisibility(View.GONE);
        }
    }

    private View getUserView(User u) {
        final LinearLayout layout = (LinearLayout)
                getActivity()
                        .getLayoutInflater()
                        .inflate(R.layout.shard_user, mCollaborators, false);
        layout.setId(View.generateViewId());
        final NetworkImageView imageView = ButterKnife.findById(layout, R.id.user_avatar);
        imageView.setId(View.generateViewId());
        imageView.setImageUrl(u.getAvatarUrl());
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        final TextView login = ButterKnife.findById(layout, R.id.user_login);
        login.setId(View.generateViewId());
        if(u.getContributions() > 0) {
            login.setText(String.format(Locale.getDefault(), "%1$s\n%2$d", u.getLogin(),
                    u.getContributions()
            ));
        } else {
            login.setText(u.getLogin());
        }
        layout.setOnClickListener((v) -> {
            final Intent us = new Intent(getActivity(), UserActivity.class);
            us.putExtra(getString(R.string.intent_username), u.getLogin());

            if(imageView.getDrawable() != null) {
                us.putExtra(getString(R.string.intent_drawable),
                        ((BitmapDrawable) imageView.getDrawable()).getBitmap()
                );
            }
            getActivity().startActivity(us,
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                            getActivity(),
                            Pair.create(login, getString(R.string.transition_username)),
                            Pair.create(imageView,
                                    getString(R.string.transition_user_image)
                            )
                    ).toBundle()
            );
        });
        return layout;
    }

    @OnClick({R.id.repo_license, R.id.repo_license_drawable, R.id.repo_license_text})
    void showLicense() {
        if(mRepo.hasLicense()) {
            final ProgressDialog pd = new ProgressDialog(getContext());
            pd.setTitle(R.string.title_loading_license);
            pd.setMessage(mRepo.getLicenseName());
            pd.show();
            mLoader.loadLicenseBody(new Loader.ItemLoader<String>() {
                @Override
                public void loadComplete(String data) {
                    pd.dismiss();
                    new AlertDialog.Builder(getContext())
                            .setTitle(mRepo.getLicenseName())
                            .setMessage(data)
                            .setPositiveButton(R.string.action_ok, null)
                            .create()
                            .show();
                }

                @Override
                public void loadError(APIHandler.APIError error) {
                    pd.dismiss();
                    Toast.makeText(getContext(), R.string.error_loading_license, Toast.LENGTH_SHORT)
                         .show();
                }
            }, mRepo.getLicenseUrl());
        }
    }

    @OnClick({R.id.user_avatar, R.id.user_name})
    void openUser() {
        if(mRepo != null) {
            final Intent i = new Intent(getContext(), UserActivity.class);
            i.putExtra(getString(R.string.intent_username), mRepo.getUserLogin());
            if(mAvatar.getDrawable() != null) {
                i.putExtra(getString(R.string.intent_drawable),
                        ((BitmapDrawable) mAvatar.getDrawable()).getBitmap()
                );
            }
            startActivity(i,
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                            getActivity(),
                            Pair.create(mUserName, getString(R.string.transition_username)),
                            Pair.create(mAvatar, getString(R.string.transition_user_image))
                    ).toBundle()
            );
        }
    }

    @OnClick(R.id.repo_show_files)
    void showFiles() {
        if(mRepo != null) {
            final Intent i = new Intent(getContext(), ContentActivity.class);
            i.putExtra(getString(R.string.intent_repo), mRepo.getFullName());
            startActivity(i);
        }

    }

    @Override
    public void notifyBackPressed() {

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
