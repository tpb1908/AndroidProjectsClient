package com.tpb.projects.commits;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.widget.TextView;

import com.tpb.github.data.APIHandler;
import com.tpb.github.data.Loader;
import com.tpb.github.data.models.Commit;
import com.tpb.projects.R;
import com.tpb.projects.commits.fragments.CommitCommentsFragment;
import com.tpb.projects.commits.fragments.CommitInfoFragment;
import com.tpb.projects.util.CircularRevealActivity;
import com.tpb.projects.util.SettingsActivity;
import com.tpb.projects.util.UI;
import com.tpb.projects.util.fab.FloatingActionButton;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * Created by theo on 30/03/17.
 */

public class CommitActivity extends CircularRevealActivity implements Loader.ItemLoader<Commit> {
    private static final String TAG = CommitActivity.class.getSimpleName();

    @BindView(R.id.commit_hash) TextView mHash;
    @BindView(R.id.commit_comment_fab) FloatingActionButton mFab;
    @BindView(R.id.commit_fragment_tabs) TabLayout mTabs;
    @BindView(R.id.commit_content_viewpager) ViewPager mPager;

    private CommitPagerAdapter mAdapter;

    private Commit mCommit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences
                .getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        UI.setStatusBarColor(getWindow(), getResources().getColor(R.color.colorPrimaryDark));
        setContentView(R.layout.activity_commit);
        ButterKnife.bind(this);

        mAdapter = new CommitPagerAdapter(getSupportFragmentManager());

        mPager.setOffscreenPageLimit(2);
        mPager.setAdapter(mAdapter);
        mTabs.setupWithViewPager(mPager);
        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if(position == 1) {
                    mFab.show(true);
                } else {
                    mFab.hide(true);
                }
            }
        });

        final Intent launchIntent = getIntent();
        if(launchIntent.hasExtra(getString(R.string.transition_card))) {
            postponeEnterTransition();
        }
        if(launchIntent.hasExtra(getString(R.string.parcel_commit))) {
            mCommit = launchIntent.getParcelableExtra(getString(R.string.parcel_commit));
            loadComplete(mCommit);
            new Loader(this).loadCommit(this, mCommit.getFullRepoName(), mCommit.getSha());
        }

    }

    @Override
    public void loadComplete(Commit data) {
        mCommit = data;
        mHash.setText(com.tpb.github.data.Util.shortenSha(mCommit.getSha()));
        mAdapter.setCommit();
    }

    @Override
    public void loadError(APIHandler.APIError error) {

    }

    private class CommitPagerAdapter extends FragmentPagerAdapter {

        private CommitInfoFragment mInfoFragment;
        private CommitCommentsFragment mCommentsFragment;

        CommitPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        void setCommit() {
            if(mInfoFragment != null) mInfoFragment.commitLoaded(mCommit);
            if(mCommentsFragment != null) mCommentsFragment.commitLoaded(mCommit);
        }

        @Override
        public Fragment getItem(int position) {
            if(position == 0) {
                mInfoFragment = CommitInfoFragment.getInstance(CommitActivity.this);
                if(mCommit != null) mInfoFragment.commitLoaded(mCommit);
                return mInfoFragment;
            } else {
                mCommentsFragment = CommitCommentsFragment.getInstance(mFab);
                if(mCommit != null) mCommentsFragment.commitLoaded(mCommit);
                return mCommentsFragment;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if(position == 0) {
                return getString(R.string.title_commit_info);
            } else {
                return getString(R.string.title_commit_comments);
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

}
