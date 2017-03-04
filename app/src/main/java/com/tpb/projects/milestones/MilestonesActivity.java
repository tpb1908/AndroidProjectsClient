package com.tpb.projects.milestones;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.tpb.animatingrecyclerview.AnimatingRecycler;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.auth.GitHubSession;
import com.tpb.projects.data.models.Milestone;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.data.models.State;
import com.tpb.projects.editors.CircularRevealActivity;
import com.tpb.projects.util.UI;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 04/03/17.
 */

public class MilestonesActivity extends CircularRevealActivity implements Loader.GITModelsLoader<Milestone> {
    private static final String TAG = MilestonesActivity.class.getSimpleName();

    @BindView(R.id.milestones_recycler) AnimatingRecycler mRecycler;
    @BindView(R.id.milestones_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.milestones_fab) FloatingActionButton mFab;

    private Loader mLoader;
    private State mFilter = State.OPEN;

    private String mRepo;
    private Repository.AccessLevel mAccessLevel;
    private int mPage = 1;
    private boolean mMaxPageReached;
    private boolean mIsLoading;


    private MilestonesAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences.getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        UI.setStatusBarColor(getWindow(), getResources().getColor(R.color.colorPrimaryDark));
        setContentView(R.layout.activity_milestones);
        ButterKnife.bind(this);

        if(getIntent().hasExtra(getString(R.string.intent_repo))) {

            mLoader = new Loader(this);
            mRepo = getIntent().getStringExtra(getString(R.string.intent_repo));

            mRecycler.setLayoutManager(new LinearLayoutManager(this));
            mAdapter = new MilestonesAdapter(this, mRepo);
            mRecycler.setAdapter(mAdapter);

            mIsLoading = true;
            mRefresher.setOnRefreshListener(this::refresh);

            mLoader.checkIfCollaborator(new Loader.GITModelLoader<Repository.AccessLevel>() {
                @Override
                public void loadComplete(Repository.AccessLevel data) {
                    mAccessLevel = data;
                    if(mAccessLevel != Repository.AccessLevel.NONE) {
                        mFab.postDelayed(mFab::show, 300);
                        enableScrollListener(mRecycler, (LinearLayoutManager) mRecycler.getLayoutManager());
                    }
                }

                @Override
                public void loadError(APIHandler.APIError error) {

                }
            }, GitHubSession.getSession(this).getUserLogin(), mRepo);

            loadMilestones(false);

        } else {
            finish();
        }
    }

    private void enableScrollListener(RecyclerView recycler, LinearLayoutManager manager) {
        recycler.setOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(dy > 10) {
                    mFab.hide();
                } else if(dy < -10) {
                    mFab.show();
                }
                if((manager.getChildCount() + manager.findFirstVisibleItemPosition()) >= manager.getItemCount()) {
                    if(!mIsLoading && !mMaxPageReached) {
                        mPage++;
                        mRefresher.setRefreshing(true);
                        loadMilestones(false);
                    }
                }
            }
        });
    }

    private void loadMilestones(boolean resetPage) {
        mIsLoading = true;
        if(resetPage) {
            mPage = 1;
            mMaxPageReached = false;
        }
        mLoader.loadMilestones(this, mRepo, mFilter, mPage);
    }

    private void refresh() {
        mAdapter.clear();
        loadMilestones(true);
        mRefresher.setRefreshing(true);
    }

    @Override
    public void loadComplete(Milestone[] milestones) {
        if(mPage == 1) {
            mAdapter.setMilestones(milestones);
        } else {
            if(milestones.length > 0) {
                mAdapter.addMilestones(milestones);
            } else {
                mMaxPageReached = true;
            }
        }
        mIsLoading = false;
        mRefresher.setRefreshing(false);
    }

    @Override
    public void loadError(APIHandler.APIError error) {
        Log.i(TAG, "loadError: " + error.toString());
    }

    public void onToolbarBackPressed(View v) {
        onBackPressed();
    }

}
