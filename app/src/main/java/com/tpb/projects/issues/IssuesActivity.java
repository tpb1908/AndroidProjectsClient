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

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.tpb.animatingrecyclerview.AnimatingRecycler;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Editor;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.auth.GitHubSession;
import com.tpb.projects.data.models.Comment;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.project.dialogs.EditIssueDialog;
import com.tpb.projects.project.dialogs.NewCommentDialog;
import com.tpb.projects.project.dialogs.NewIssueDialog;
import com.tpb.projects.util.Analytics;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

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
    @BindView(R.id.issues_fab) FloatingActionButton mFab;

    private Loader mLoader;
    private Editor mEditor;

    private IssuesAdapter mAdapter;

    private Repository.AccessLevel mAccessLevel;
    private String mRepoPath;

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

        mAdapter = new IssuesAdapter(this);

        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setAdapter(mAdapter);
        
        mRefresher.setOnRefreshListener(() -> {
            mAdapter.clear();
            mLoader.loadIssues(IssuesActivity.this, mRepoPath);
        });

        if(getIntent().getExtras() != null && getIntent().getExtras().containsKey(getString(R.string.intent_repo))) {
            mRepoPath = getIntent().getExtras().getString(getString(R.string.intent_repo));
            mLoader.loadIssues(this, mRepoPath);
            mRefresher.setRefreshing(true);

            mLoader.checkIfCollaborator(new Loader.AccessCheckListener() {

                @Override
                public void accessCheckComplete(Repository.AccessLevel accessLevel) {
                    mAccessLevel = accessLevel;
                    if(mAccessLevel != Repository.AccessLevel.NONE) {
                        mFab.postDelayed(mFab::show, 300);

                        mRecycler.setOnScrollListener(new RecyclerView.OnScrollListener() {
                            @Override
                            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                                super.onScrolled(recyclerView, dx, dy);
                                if(dy > 10) {
                                    mFab.hide();
                                } else if(dy < -10) {
                                    mFab.show();
                                }
                            }
                        });
                    }
                }

                @Override
                public void accessCheckError(APIHandler.APIError error) {

                }
            }, GitHubSession.getSession(this).getUserLogin(), mRepoPath);

        } else {
            finish();
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

    void openIssue(Issue issue) {
        final Intent i = new Intent(IssuesActivity.this, IssueActivity.class);
        i.putExtra(getString(R.string.parcel_issue), issue);
        startActivity(i);
        overridePendingTransition(R.anim.slide_up, R.anim.none);
    }

    void openMenu(View view, final Issue issue) {
        final PopupMenu menu = new PopupMenu(this, view);
        menu.inflate(R.menu.menu_issue);
        if(mAccessLevel == Repository.AccessLevel.ADMIN || mAccessLevel == Repository.AccessLevel.WRITE) {
            menu.getMenu().add(0, 1, Menu.NONE, issue.isClosed() ? R.string.menu_reopen_issue : R.string.menu_close_issue);
            menu.getMenu().add(0, 2, Menu.NONE, getString(R.string.menu_edit_issue));
        }
        menu.setOnMenuItemClickListener(menuItem -> {
            switch(menuItem.getItemId()) {
                case 1:
                    toggleIssueState(issue);
                    break;
                case 2:
                    editIssue(issue);
                    break;
            }
            return false;
        });
        menu.show();
    }

    private void editIssue(Issue issue) {
        final EditIssueDialog editDialog = new EditIssueDialog();
        editDialog.setListener(new EditIssueDialog.EditIssueDialogListener() {
            @Override
            public void issueEdited(Issue issue, @Nullable String[] assignees, @Nullable String[] labels) {
                mRefresher.setRefreshing(true);
                mEditor.editIssue(new Editor.IssueEditListener() {
                    int issueCreationAttempts = 0;

                    @Override
                    public void issueEdited(Issue issue) {
                        mAdapter.updateIssue(issue);
                        mRefresher.setRefreshing(false);
                        final Bundle bundle = new Bundle();
                        bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                        mAnalytics.logEvent(Analytics.TAG_ISSUE_EDIT, bundle);
                    }

                    @Override
                    public void issueEditError(APIHandler.APIError error) {
                        if(error == APIHandler.APIError.NO_CONNECTION) {
                            mRefresher.setRefreshing(false);
                            Toast.makeText(IssuesActivity.this, error.resId, Toast.LENGTH_SHORT).show();
                        } else {
                            if(issueCreationAttempts < 5) {
                                issueCreationAttempts++;
                                mEditor.editIssue(this, mRepoPath, issue, assignees, labels);
                            } else {
                                Toast.makeText(IssuesActivity.this, error.resId, Toast.LENGTH_SHORT).show();
                                mRefresher.setRefreshing(false);
                            }
                        }

                        final Bundle bundle = new Bundle();
                        bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                        mAnalytics.logEvent(Analytics.TAG_ISSUE_EDIT, bundle);
                    }
                }, mRepoPath, issue, assignees, labels);
            }

            @Override
            public void issueEditCancelled() {

            }
        });
        final Bundle c = new Bundle();
        c.putParcelable(getString(R.string.parcel_issue), issue);
        c.putString(getString(R.string.intent_repo), issue.getRepoPath());
        editDialog.setArguments(c);
        editDialog.show(getSupportFragmentManager(), TAG);
    }

    private void toggleIssueState(Issue issue) {
        final Editor.IssueStateChangeListener listener = new Editor.IssueStateChangeListener() {
            @Override
            public void issueStateChanged(Issue issue) {
                mAdapter.updateIssue(issue);
                mRefresher.setRefreshing(false);
                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                mAnalytics.logEvent(issue.isClosed() ? Analytics.TAG_ISSUE_CLOSED : Analytics.TAG_ISSUE_OPENED, bundle);
            }

            @Override
            public void issueStateChangeError(APIHandler.APIError error) {
                mRefresher.setRefreshing(false);
                if(error == APIHandler.APIError.NO_CONNECTION) {
                    Toast.makeText(IssuesActivity.this, error.resId, Toast.LENGTH_SHORT).show();
                }
                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                mAnalytics.logEvent(Analytics.TAG_ISSUE_EDIT, bundle);
            }
        };

        final NewCommentDialog dialog = new NewCommentDialog();
        dialog.setListener(new NewCommentDialog.NewCommentDialogListener() {
            @Override
            public void commentCreated(String body) {
                mEditor.createComment(new Editor.CommentCreationListener() {
                    @Override
                    public void commentCreated(Comment comment) {
                        mRefresher.setRefreshing(true);
                        if(issue.isClosed()) {
                            mEditor.openIssue(listener, issue.getRepoPath(), issue.getNumber());
                        } else {
                            mEditor.closeIssue(listener, issue.getRepoPath(), issue.getNumber());
                        }
                    }

                    @Override
                    public void commentCreationError(APIHandler.APIError error) {
                        mRefresher.setRefreshing(false);
                        if(error == APIHandler.APIError.NO_CONNECTION) {
                            Toast.makeText(IssuesActivity.this, error.resId, Toast.LENGTH_SHORT).show();
                        }
                    }
                }, issue.getRepoPath(), issue.getNumber(), body);

            }

            @Override
            public void commentNotCreated() {
                mRefresher.setRefreshing(true);
                if(issue.isClosed()) {
                    mEditor.openIssue(listener, issue.getRepoPath(), issue.getNumber());
                } else {
                    mEditor.closeIssue(listener, issue.getRepoPath(), issue.getNumber());
                }
            }
        });
        dialog.show(getSupportFragmentManager(), TAG);
    }

    @OnClick(R.id.issues_fab)
    public void createNewIssue() {
        final NewIssueDialog newDialog = new NewIssueDialog();
        newDialog.setListener(new NewIssueDialog.IssueDialogListener() {
            @Override
            public void issueCreated(Issue issue) {
                mAdapter.addIssue(issue);
                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                mAnalytics.logEvent(Analytics.TAG_ISSUE_CREATED, bundle);
            }

            @Override
            public void issueCreationCancelled() {
            }
        });
        final Bundle c = new Bundle();
        c.putString(getString(R.string.intent_repo), mRepoPath);
        newDialog.setArguments(c);
        newDialog.show(getSupportFragmentManager(), TAG);
    }

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }
}
