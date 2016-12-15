package com.tpb.projects.feed;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import com.androidnetworking.AndroidNetworking;
import com.tpb.projects.R;
import com.tpb.projects.data.auth.GitHubApp;
import com.tpb.projects.user.LoginActivity;
import com.tpb.projects.util.Constants;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 14/12/16.
 * Possible markdown textviews
 *
 *https://github.com/fiskurgit/MarkdownView
 *https://github.com/mukeshsolanki/MarkdownView-Android
 * https://github.com/mittsuu/MarkedView-for-Android
 * https://github.com/falnatsheh/MarkdownView best demo
 *
 */

public class ReposActivity extends AppCompatActivity {

    private GitHubApp mApp;
    @BindView(R.id.repo_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.repo_recycler) RecyclerView mRecycler;
    @BindView(R.id.repo_toolbar) Toolbar mToolbar;
    @BindView(R.id.repo_appbar) AppBarLayout mAppbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repos);
        ButterKnife.bind(this);
        AndroidNetworking.initialize(this);
        mApp = new GitHubApp(this, Constants.CLIENT_ID, Constants.CLIENT_SECRET, Constants.REDIRECT_URL);
        if(!mApp.hasAccessToken()) {
            startActivity(new Intent(ReposActivity.this, LoginActivity.class));
        }
    }
}
