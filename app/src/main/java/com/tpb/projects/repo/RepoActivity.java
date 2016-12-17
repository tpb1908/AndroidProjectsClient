package com.tpb.projects.repo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.androidnetworking.widget.ANImageView;
import com.tpb.projects.R;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.auth.models.Repository;
import com.tpb.projects.util.Constants;
import com.tpb.projects.util.Data;

import butterknife.BindView;
import butterknife.ButterKnife;
import us.feras.mdv.MarkdownView;

/**
 * Created by theo on 16/12/16.
 */

public class RepoActivity extends AppCompatActivity implements Loader.RepositoryLoader{

    @BindView(R.id.repo_name) TextView mName;
    @BindView(R.id.repo_description) TextView mDescription;
    @BindView(R.id.user_image) ANImageView mUserImage;
    @BindView(R.id.user_name) TextView mUserName;

    @BindView(R.id.repo_forks) TextView mForks;
    @BindView(R.id.repo_size) TextView mSize;
    @BindView(R.id.repo_stars) TextView mStars;
    @BindView(R.id.repo_watchers) TextView mWatchers;

    @BindView(R.id.repo_readme) MarkdownView mReadMe;

    @BindView(R.id.repo_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.repo_project_recycler) RecyclerView mRecycler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme_Dark);
        setContentView(R.layout.activity_repo);
        ButterKnife.bind(this);
        final Intent launchIntent = getIntent();
        if(launchIntent.getParcelableExtra(getString(R.string.intent_repo)) != null) {
            repoLoaded(launchIntent.getParcelableExtra(getString(R.string.intent_repo)));
        } else {
            //TODO Begin loading repo from url
        }
        mRefresher.setRefreshing(true);

    }

    @Override
    public void repoLoaded(Repository repo) {
        mName.setText(repo.getName());
        if(Constants.JSON_NULL.equals(repo.getDescription())) {
            mDescription.setVisibility(View.GONE);
        } else {
            mDescription.setText(repo.getDescription());
        }
        mUserName.setText(repo.getUserLogin());
        mUserImage.setImageUrl(repo.getUserAvatarUrl());
        mSize.setText(Data.formatKB(repo.getSize()));
        mForks.setText(Integer.toString(repo.getForks()));
        mWatchers.setText(Integer.toString(repo.getWatchers()));
        mStars.setText(Integer.toString(repo.getStarGazers()));
    }

    @Override
    public void loadError() {

    }
}
