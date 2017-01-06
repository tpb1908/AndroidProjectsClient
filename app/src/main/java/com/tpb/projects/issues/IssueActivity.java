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
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

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
 * Created by theo on 06/01/17.
 */

public class IssueActivity extends AppCompatActivity implements Loader.IssueLoader{
    private static final String TAG = IssueActivity.class.getSimpleName();

    @BindView(R.id.issue_appbar) AppBarLayout mAppbar;
    @BindView(R.id.issue_toolbar) Toolbar mToolbar;
    @BindView(R.id.issue_number) TextView mNumber;
    @BindView(R.id.issue_scrollview) NestedScrollView mScrollView;
    @BindView(R.id.issue_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.issue_comments_recycler) AnimatingRecycler mRecycler;
    @BindView(R.id.issue_comment_fab) FloatingActionButton mFab;

    private Editor mEditor;
    private Loader mLoader;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences.getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        setContentView(R.layout.activity_issue);
        ButterKnife.bind(this);

        mEditor = new Editor(this);
        mLoader = new Loader(this);

        if(getIntent().getExtras() != null && getIntent().getExtras().containsKey(getString(R.string.parcel_issue))) {
            final Issue issue = getIntent().getExtras().getParcelable(getString(R.string.parcel_issue));
            mNumber.setText(String.format("#%1$d", issue.getNumber()));
            issueLoaded(issue);
        } else {
            final int issueNumber = getIntent().getIntExtra(getString(R.string.intent_issue_number), -1);
            final String repoName = getIntent().getStringExtra(getString(R.string.intent_repo));
            mNumber.setText(String.format("#%1$d", issueNumber));
            mLoader.loadIssue(this, repoName, issueNumber);
        }
    }

    @Override
    public void issueLoaded(Issue issue) {

    }

    @Override
    public void issueLoadError(APIHandler.APIError error) {

    }

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }

}
