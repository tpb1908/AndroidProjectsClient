package com.tpb.projects.feed;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.androidnetworking.AndroidNetworking;
import com.tpb.projects.R;
import com.tpb.projects.data.auth.OAuthLoader;
import com.tpb.projects.data.auth.models.Repository;
import com.tpb.projects.repo.RepoActivity;
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

public class ReposActivity extends AppCompatActivity implements ReposAdapter.RepoOpener {
    private static final String TAG = ReposActivity.class.getSimpleName();

    private OAuthLoader mApp;
    @BindView(R.id.repos_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.repos_recycler) RecyclerView mRecycler;
    @BindView(R.id.repos_toolbar) Toolbar mToolbar;
    @BindView(R.id.repos_appbar) AppBarLayout mAppbar;

    private ReposAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme_Dark);
        setContentView(R.layout.activity_repos);

        ButterKnife.bind(this);
        AndroidNetworking.initialize(this);
        mApp = new OAuthLoader(this, Constants.CLIENT_ID, Constants.CLIENT_SECRET, Constants.REDIRECT_URL);
        if(!mApp.hasAccessToken()) {
            startActivity(new Intent(ReposActivity.this, LoginActivity.class));
        }

        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new ReposAdapter(this, this, mRecycler, mRefresher);
        mRecycler.setAdapter(mAdapter);

    }

    @Override
    public void openRepo(Repository repo, View view) {
        final Intent i = new Intent(ReposActivity.this, RepoActivity.class);
        i.putExtra(getString(R.string.intent_repo), repo);
        startActivity(i, ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,
                Pair.create(view, getString(R.string.transition_card))
                ).toBundle()
        );
        overridePendingTransition(R.anim.slide_up, R.anim.none);
    }
}
