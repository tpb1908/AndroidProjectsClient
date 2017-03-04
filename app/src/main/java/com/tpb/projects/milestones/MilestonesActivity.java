package com.tpb.projects.milestones;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.View;

import com.tpb.animatingrecyclerview.AnimatingRecycler;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.models.Milestone;
import com.tpb.projects.editors.CircularRevealActivity;
import com.tpb.projects.util.UI;

import java.util.ArrayList;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 04/03/17.
 */

public class MilestonesActivity extends CircularRevealActivity {
    private static final String TAG = MilestonesActivity.class.getSimpleName();

    @BindView(R.id.milestones_recycler) AnimatingRecycler mRecycler;
    @BindView(R.id.milestones_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.milestones_fab) FloatingActionButton mFab;

    private String mRepo;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences.getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        UI.setStatusBarColor(getWindow(), getResources().getColor(R.color.colorPrimaryDark));
        setContentView(R.layout.activity_milestones);
        ButterKnife.bind(this);

        if(getIntent().hasExtra(getString(R.string.intent_repo))) {
            mRepo = getIntent().getStringExtra(getString(R.string.intent_repo));
            new Loader(this).loadMilestones(new Loader.GITModelsLoader<Milestone>() {
                @Override
                public void loadComplete(Milestone[] data) {
                    Log.i(TAG, "loadComplete: Milestones loaded " + Arrays.toString(data));
                    mRecycler.setLayoutManager(new LinearLayoutManager(MilestonesActivity.this));
                    final MilestonesAdapter adapter = new MilestonesAdapter(MilestonesActivity.this, mRepo);
                    adapter.setMilestones(new ArrayList<>(Arrays.asList(data)));
                    mRecycler.setAdapter(adapter);
                }

                @Override
                public void loadError(APIHandler.APIError error) {

                }
            }, mRepo);
        } else {
            finish();
        }
    }

    public void onToolbarBackPressed(View v) {
        onBackPressed();
    }

}
