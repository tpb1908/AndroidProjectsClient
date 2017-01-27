/*
 * Copyright  2016 Theo Pearson-Bray
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.tpb.projects.issues;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.tpb.animatingrecyclerview.AnimatingRecycler;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Editor;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.models.Issue;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 27/01/17.
 */

public class IssuesActivity extends AppCompatActivity implements Loader.IssuesLoader {
    private static final String TAG = IssuesActivity.class.getSimpleName();

    private FirebaseAnalytics mAnalytics;

    @BindView(R.id.issues_appbar) AppBarLayout mAppbar;
    @BindView(R.id.issues_toolbar) Toolbar mToolbar;
    @BindView(R.id.issues_recycler) AnimatingRecycler mRecycler;
    @BindView(R.id.issues_refresher) SwipeRefreshLayout mRefresher;

    private Loader mLoader;
    private Editor mEditor;

    private IssuesAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences.getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        setContentView(R.layout.activity_issues);
        ButterKnife.bind(this);
        mAnalytics = FirebaseAnalytics.getInstance(this);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mLoader = new Loader(this);
        mEditor = new Editor(this);

        mAdapter = new IssuesAdapter();

        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setAdapter(mAdapter);

        if(getIntent().getExtras() != null && getIntent().getExtras().containsKey(getString(R.string.intent_repo))) {
            mLoader.loadIssues(this, getIntent().getExtras().getString(getString(R.string.intent_repo)));
            mRefresher.setRefreshing(true);
        }

    }
    @Override
    public void issuesLoaded(Issue[] issues) {
        mAdapter.loadIssues(issues);
        mRefresher.setRefreshing(false);
    }

    @Override
    public void issuesLoadError(APIHandler.APIError error) {

    }

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }
}
