package com.tpb.projects.feed;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.widget.ANImageView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.tpb.animatingrecyclerview.AnimatingRecycler;
import com.tpb.projects.R;
import com.tpb.projects.data.auth.OAuthHandler;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.repo.RepoActivity;
import com.tpb.projects.user.LoginActivity;
import com.tpb.projects.util.Analytics;
import com.tpb.projects.util.Constants;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 14/12/16.
 * Possible markdown textviews
 * <p>
 * https://github.com/fiskurgit/MarkdownView
 * https://github.com/mukeshsolanki/MarkdownView-Android
 * https://github.com/mittsuu/MarkedView-for-Android
 * https://github.com/falnatsheh/MarkdownView best demo
 */

public class ReposActivity extends AppCompatActivity implements ReposAdapter.ReposManager {
    private static final String TAG = ReposActivity.class.getSimpleName();
    private FirebaseAnalytics mAnalytics;

    private OAuthHandler mApp;
    @BindView(R.id.repos_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.repos_recycler) AnimatingRecycler mRecycler;
    @BindView(R.id.repos_toolbar) Toolbar mToolbar;
    @BindView(R.id.repos_appbar) AppBarLayout mAppbar;

    @BindView(R.id.user_image) ANImageView mUserAvatar;
    @BindView(R.id.user_name) TextView mUserName;

    private ReposAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme_Dark);
        setContentView(R.layout.activity_repos);

        ButterKnife.bind(this);
        AndroidNetworking.initialize(this);

        mAnalytics = FirebaseAnalytics.getInstance(this);
        mAnalytics.setAnalyticsCollectionEnabled(true);
        mAnalytics.logEvent(Analytics.TAG_OPEN_REPOS_ACTIVITY, null);

        mApp = new OAuthHandler(this, Constants.CLIENT_ID, Constants.CLIENT_SECRET, Constants.REDIRECT_URL);
        if(!mApp.hasAccessToken()) {
            startActivity(new Intent(ReposActivity.this, LoginActivity.class));
        }
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new ReposAdapter(this, this, mRecycler, mRefresher);
        mRecycler.setAdapter(mAdapter);
        mUserName.setText(mApp.getUserName());
        mApp.validateKey(isValid -> {
            if(!isValid) {
                Toast.makeText(ReposActivity.this, R.string.error_key_invalid, Toast.LENGTH_LONG).show();
                startActivity(new Intent(ReposActivity.this, LoginActivity.class));
            }
        });
    }

    @Override
    public void openRepo(Repository repo, View view) {
        final Intent i = new Intent(ReposActivity.this, RepoActivity.class);
        i.putExtra(getString(R.string.intent_repo), repo);
        mRecycler.disableAnimation();
        startActivity(i, ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,
                view,
                getString(R.string.transition_name)
                ).toBundle()
        );
        overridePendingTransition(R.anim.slide_up, R.anim.none);
        mAnalytics.logEvent(Analytics.TAG_OPEN_REPO, null);
    }

    @Override
    public void displayUserAvatar(String userImagePath) {
        mUserAvatar.setImageUrl(userImagePath);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


}
