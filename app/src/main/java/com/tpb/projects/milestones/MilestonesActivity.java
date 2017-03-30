package com.tpb.projects.milestones;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.PopupMenu;

import com.tpb.animatingrecyclerview.AnimatingRecyclerView;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Editor;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.auth.GitHubSession;
import com.tpb.projects.data.models.Milestone;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.data.models.State;
import com.tpb.projects.editors.MilestoneEditor;
import com.tpb.projects.util.CircularRevealActivity;
import com.tpb.projects.util.UI;
import com.tpb.projects.util.Util;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by theo on 04/03/17.
 */

public class MilestonesActivity extends CircularRevealActivity implements Loader.ListLoader<Milestone> {
    private static final String TAG = MilestonesActivity.class.getSimpleName();

    @BindView(R.id.milestones_recycler) AnimatingRecyclerView mRecycler;
    @BindView(R.id.milestones_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.milestones_fab) com.tpb.projects.util.fab.FloatingActionButton mFab;

    private Loader mLoader;
    private Editor mEditor;
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
            mEditor = new Editor(this);
            mRepo = getIntent().getStringExtra(getString(R.string.intent_repo));

            mRecycler.setLayoutManager(new LinearLayoutManager(this));
            mAdapter = new MilestonesAdapter(this, mRepo);
            mRecycler.setAdapter(mAdapter);

            mIsLoading = true;
            mRefresher.setOnRefreshListener(this::refresh);

            mLoader.checkIfCollaborator(new Loader.ItemLoader<Repository.AccessLevel>() {
                @Override
                public void loadComplete(Repository.AccessLevel data) {
                    mAccessLevel = data;
                    if(mAccessLevel != Repository.AccessLevel.NONE) {
                        mFab.postDelayed(() -> mFab.show(true), 300);
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
                    mFab.hide(true);
                } else if(dy < -10) {
                    mFab.show(true);
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
        Log.i(TAG, "loadMilestones: Loading with filter " + mFilter);
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

    @OnClick(R.id.milestones_filter_button)
    void filter(View v) {
        final PopupMenu menu = new PopupMenu(this, v);
        menu.inflate(R.menu.menu_milestones_filter);
        switch(mFilter) {
            case ALL:
                menu.getMenu().getItem(2).setChecked(true);
                break;
            case OPEN:
                menu.getMenu().getItem(0).setChecked(true);
                break;
            case CLOSED:
                menu.getMenu().getItem(1).setChecked(true);
                break;
        }
        menu.setOnMenuItemClickListener(menuItem -> {
            switch(menuItem.getItemId()) {
                case R.id.menu_filter_all:
                    mFilter = State.ALL;
                    refresh();
                    break;
                case R.id.menu_filter_closed:
                    mFilter = State.CLOSED;
                    refresh();
                    break;
                case R.id.menu_filter_open:
                    mFilter = State.OPEN;
                    refresh();
                    break;
            }
            return false;
        });
        menu.show();
    }

    @OnClick(R.id.milestones_fab)
    void newMilestone() {
        final Intent i = new Intent(MilestonesActivity.this, MilestoneEditor.class);
        i.putExtra(getString(R.string.intent_repo), mRepo);
        UI.setViewPositionForIntent(i, mFab);
        startActivityForResult(i, MilestoneEditor.REQUEST_CODE_NEW_MILESTONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK) {
            mRefresher.setRefreshing(true);
            final String title = data.getStringExtra(getString(R.string.intent_milestone_title));
            final String description = data.getStringExtra(getString(R.string.intent_milestone_description));
            final int number = data.getIntExtra(getString(R.string.intent_milestone_number), -1);
            final long dueOn = data.getLongExtra(getString(R.string.intent_milestone_due_on), -1);

            if(requestCode == MilestoneEditor.REQUEST_CODE_NEW_MILESTONE) {
                mEditor.createMilestone(new Editor.CreationListener<Milestone>() {
                    @Override
                    public void created(Milestone milestone) {
                        Log.i(TAG, "created: Milestone created");
                        mRefresher.setRefreshing(false);
                        mAdapter.addMilestone(milestone);
                        mRecycler.scrollToPosition(0);
                    }

                    @Override
                    public void creationError(APIHandler.APIError error) {
                        mRefresher.setRefreshing(false);
                    }
                }, mRepo, title, description, dueOn > 0 ? Util.toISO8061FromMilliseconds(dueOn) : null);
            } else if(requestCode == MilestoneEditor.REQUEST_CODE_EDIT_MILESTONE) {
                mEditor.updateMilestone(new Editor.UpdateListener<Milestone>() {
                    @Override
                    public void updated(Milestone milestone) {
                        mRefresher.setRefreshing(false);
                        mAdapter.updateMilestone(milestone);
                    }

                    @Override
                    public void updateError(APIHandler.APIError error) {
                        mRefresher.setRefreshing(false);
                    }
                }, mRepo, number, title, description, Util.toISO8061FromMilliseconds(dueOn), null);
            }
        }
    }

}
