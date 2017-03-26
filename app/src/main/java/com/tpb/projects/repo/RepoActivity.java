package com.tpb.projects.repo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.widget.TextView;

import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.repo.fragments.RepoFragment;
import com.tpb.projects.repo.fragments.RepoInfoFragment;
import com.tpb.projects.repo.fragments.RepoIssuesFragment;
import com.tpb.projects.repo.fragments.RepoProjectsFragment;
import com.tpb.projects.util.BaseActivity;
import com.tpb.projects.util.UI;
import com.tpb.projects.util.fab.FloatingActionButton;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 25/03/17.
 */

public class RepoActivity extends BaseActivity implements Loader.GITModelLoader<Repository> {

    public static final int PAGE_ISSUES = 1;
    public static final int PAGE_PROJECTS = 2;

    @BindView(R.id.title_repo) TextView mTitle;
    @BindView(R.id.repo_fragment_tabs) TabLayout mTabs;
    @BindView(R.id.repo_fragment_viewpager) ViewPager mPager;
    @BindView(R.id.repo_fab) FloatingActionButton mFab;

    private RepoFragmentAdapter mAdapter;
    private Repository mRepo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences.getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        UI.setStatusBarColor(getWindow(), getResources().getColor(R.color.colorPrimaryDark));
        setContentView(R.layout.activity_repo);
        ButterKnife.bind(this);

        if(mAdapter == null) mAdapter = new RepoFragmentAdapter(getSupportFragmentManager());
        mTabs.setupWithViewPager(mPager);
        mPager.setAdapter(mAdapter);
        mPager.setOffscreenPageLimit(3);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mAdapter.mFragments[position].handleFab(mFab);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        final Intent launchIntent = getIntent();
        final Loader loader = new Loader(this);
        if(launchIntent.getParcelableExtra(getString(R.string.intent_repo)) != null) {
            mRepo = launchIntent.getParcelableExtra(getString(R.string.intent_repo));
            if(mRepo.isFork()) {
                loader.loadRepository(this, mRepo.getFullName());
            } else {
                loadComplete(launchIntent.getParcelableExtra(getString(R.string.intent_repo)));
            }
        } else {
            loader.loadRepository(this, launchIntent.getStringExtra(getString(R.string.intent_repo)));
        }
    }

    @Override
    public void loadComplete(Repository repo) {
        mRepo = repo;
        mAdapter.notifyRepoLoaded();
        mTitle.setText(repo.getName());
    }

    @Override
    public void loadError(APIHandler.APIError error) {

    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if(mAdapter == null) mAdapter = new RepoFragmentAdapter(getSupportFragmentManager());
        if(fragment instanceof RepoFragment) mAdapter.ensureAttached((RepoFragment) fragment);
    }

    @Override
    public void onBackPressed() {
        mAdapter.notifyBackPressed();
        super.onBackPressed();
    }

    private class RepoFragmentAdapter extends FragmentPagerAdapter {

        private RepoFragment[] mFragments = new RepoFragment[3];

        RepoFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 3;
        }

        void ensureAttached(RepoFragment fragment) {
            if(fragment instanceof RepoInfoFragment) mFragments[0] = fragment;
            if(fragment instanceof RepoIssuesFragment) mFragments[1] = fragment;
            if(fragment instanceof RepoProjectsFragment) mFragments[2] = fragment;
        }

        void notifyRepoLoaded() {
            for(RepoFragment rf : mFragments) {
                if(rf != null) rf.repoLoaded(mRepo);
            }
        }

        void notifyBackPressed() {
            for(RepoFragment rf : mFragments) {
                if(rf != null) rf.notifyBackPressed();
            }
        }

        @Override
        public Fragment getItem(int position) {
            switch(position) {
                case 0:
                    mFragments[0] = new RepoInfoFragment();
                    break;
                case 1:
                    mFragments[1] = new RepoIssuesFragment();
                    break;
                case 2:
                    mFragments[2] = new RepoProjectsFragment();
                    break;
            }
            if(mRepo != null) mFragments[position].repoLoaded(mRepo);
            return mFragments[position];
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch(position) {
                case 0: return getString(R.string.title_repo_info);
                case 1: return getString(R.string.title_repo_issues);
                case 2: return getString(R.string.title_repo_projects);
                default: return "";
            }
        }
    }



}
