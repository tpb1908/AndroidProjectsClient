package com.tpb.projects.commits;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.widget.TextView;

import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.models.Commit;
import com.tpb.projects.util.CircularRevealActivity;
import com.tpb.projects.util.UI;
import com.tpb.projects.util.fab.FloatingActionButton;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 30/03/17.
 */

public class CommitActivity extends CircularRevealActivity implements Loader.ItemLoader<Commit> {

    @BindView(R.id.commit_hash) TextView mHash;
    @BindView(R.id.commit_comment_fab) FloatingActionButton mFab;
    @BindView(R.id.commit_fragment_tabs) TabLayout mTabs;
    @BindView(R.id.commit_content_viewpager) ViewPager mPager;

    private Commit mCommit;
    private boolean mHadInitialCommit = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences.getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        UI.setStatusBarColor(getWindow(), getResources().getColor(R.color.colorPrimaryDark));
        setContentView(R.layout.activity_commit);
        ButterKnife.bind(this);

        final Intent launchIntent = getIntent();
        if(launchIntent.hasExtra(getString(R.string.parcel_commit))) {
            mCommit = launchIntent.getParcelableExtra(getString(R.string.parcel_commit));
            mHadInitialCommit = true;
            loadComplete(mCommit);
            Log.i(this.getClass().getSimpleName(), "onCreate: Repo url is " + mCommit.getFullRepoName());
            new Loader(this).loadCommit(this, mCommit.getFullRepoName(), mCommit.getSha());
        }

    }

    @Override
    public void loadComplete(Commit data) {
        if(mHadInitialCommit) {
            //TODO Only bind diff info
            mHadInitialCommit = false;
        } else {
            //TODO Bind everything
        }
    }

    @Override
    public void loadError(APIHandler.APIError error) {

    }
}
