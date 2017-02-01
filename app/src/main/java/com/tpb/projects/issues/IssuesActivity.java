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

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
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
import com.tpb.projects.data.models.Label;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.data.models.User;
import com.tpb.projects.dialogs.CommentDialog;
import com.tpb.projects.dialogs.EditIssueDialog;
import com.tpb.projects.dialogs.MultiChoiceDialog;
import com.tpb.projects.dialogs.NewIssueDialog;
import com.tpb.projects.util.Analytics;
import com.tpb.projects.util.ShortcutDialog;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by theo on 27/01/17.
 */

public class IssuesActivity extends AppCompatActivity implements Loader.IssuesLoader {
    private static final String TAG = IssuesActivity.class.getSimpleName();
    private static final String URL = "https://github.com/tpb1908/AndroidProjectsClient/blob/master/app/src/main/java/com/tpb/projects/issues/IssuesActivity.java";

    private FirebaseAnalytics mAnalytics;

    @BindView(R.id.issues_appbar) AppBarLayout mAppbar;
    @BindView(R.id.issues_toolbar) Toolbar mToolbar;
    @BindView(R.id.issues_recycler) AnimatingRecycler mRecycler;
    @BindView(R.id.issues_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.issues_fab) FloatingActionButton mFab;
    @BindView(R.id.issues_filter_button) ImageButton mFilterButton;

    private Loader mLoader;
    private Editor mEditor;

    private IssuesAdapter mAdapter;
    private Issue.IssueState mFilter = Issue.IssueState.OPEN;
    private String mAssigneeFilter;
    private ArrayList<String> mLabelsFilter = new ArrayList<>();

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
        
        mRefresher.setOnRefreshListener(this::refresh);

        if(getIntent().getExtras() != null && getIntent().getExtras().containsKey(getString(R.string.intent_repo))) {
            mRepoPath = getIntent().getExtras().getString(getString(R.string.intent_repo));
            loadIssues();
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

    private void loadIssues() {
        //TODO Add an option for all, and anything
        if(mAssigneeFilter == null || mAssigneeFilter.equals(getString(R.string.text_assignee_all))) {
            mLoader.loadIssues(IssuesActivity.this, mRepoPath, mFilter, null, mLabelsFilter);
        } else if(mAssigneeFilter.equals(getString(R.string.text_assignee_none))) {
            mLoader.loadIssues(IssuesActivity.this, mRepoPath, mFilter, "none", mLabelsFilter);
        } else {
            mLoader.loadIssues(IssuesActivity.this, mRepoPath, mFilter, mAssigneeFilter, mLabelsFilter);
        }
    }

    private void refresh() {
        mAdapter.clear();
        loadIssues();
        mRefresher.setRefreshing(true);
    }

    @Override
    public void issuesLoaded(Issue[] issues) {
        mAdapter.loadIssues(issues);
        mRefresher.setRefreshing(false);
    }

    @Override
    public void issuesLoadError(APIHandler.APIError error) {

    }

    @OnClick(R.id.issues_filter_button)
    void filter() {
        final PopupMenu menu = new PopupMenu(this, mFilterButton);
        menu.inflate(R.menu.menu_issues_filter);
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

                case R.id.menu_filter_assignees:
                    showAssigneesDialog();
                    break;
                case R.id.menu_filter_labels:
                    showLabelsDialog();
                    break;
                case R.id.menu_filter_all:
                    mFilter = Issue.IssueState.ALL;
                    refresh();
                    break;
                case R.id.menu_filter_closed:
                    mFilter = Issue.IssueState.CLOSED;
                    refresh();
                    break;
                case R.id.menu_filter_open:
                    mFilter = Issue.IssueState.OPEN;
                    refresh();
                    break;
            }
            return false;
        });
        menu.show();
    }

    private void showLabelsDialog() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle(R.string.text_loading_labels);
        pd.setCancelable(false);
        pd.show();
        mLoader.loadLabels(new Loader.LabelsLoader() {
            @Override
            public void labelsLoaded(Label[] labels) {
                final MultiChoiceDialog mcd = new MultiChoiceDialog();

                final Bundle b = new Bundle();
                b.putInt(getString(R.string.intent_title_res), R.string.title_choose_labels);
                mcd.setArguments(b);

                final String[] labelTexts = new String[labels.length];
                final int[] colors = new int[labels.length];
                final boolean[] choices = new boolean[labels.length];
                for(int i = 0; i < labels.length; i++) {
                    labelTexts[i] = labels[i].getName();
                    colors[i] = labels[i].getColor();
                    choices[i] = mLabelsFilter.indexOf(labels[i].getName()) != -1;
                }


                mcd.setChoices(labelTexts, choices);
                mcd.setTextColors(colors);
                mcd.setListener(new MultiChoiceDialog.MultiChoiceDialogListener() {
                    @Override
                    public void ChoicesComplete(String[] choices, boolean[] checked) {
                        mLabelsFilter.clear();
                        for(int i = 0; i < choices.length; i++) {
                            if(checked[i]) {
                                mLabelsFilter.add(choices[i]);
                            }
                        }
                        refresh();
                    }

                    @Override
                    public void ChoicesCancelled() {

                    }
                });
                pd.dismiss();
                mcd.show(getSupportFragmentManager(), TAG);
            }

            @Override
            public void labelLoadError(APIHandler.APIError error) {
                Toast.makeText(IssuesActivity.this, error.resId, Toast.LENGTH_SHORT).show();
            }
        }, mRepoPath);
    }

    private void showAssigneesDialog() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle(R.string.text_loading_collaborators);
        pd.setCancelable(false);
        pd.show();
        mLoader.loadCollaborators(new Loader.CollaboratorsLoader() {
            @Override
            public void collaboratorsLoaded(User[] collaborators) {

                final String[] collabNames = new String[collaborators.length + 2];
                collabNames[0] = getString(R.string.text_assignee_all);
                collabNames[1] = getString(R.string.text_assignee_none);
                int pos = 0;
                for(int i = 2; i < collabNames.length; i++) {
                    collabNames[i] = collaborators[i - 2].getLogin();
                    if(collabNames[i].equals(mAssigneeFilter)) {
                        pos = i;
                    }
                }

                final AlertDialog.Builder builder = new AlertDialog.Builder(IssuesActivity.this);
                builder.setTitle(R.string.title_choose_assignee);
                builder.setSingleChoiceItems(collabNames, pos, (dialogInterface, i) -> {
                    mAssigneeFilter = collabNames[i];
                });
                builder.setPositiveButton(R.string.action_ok, (dialogInterface, i) -> {
                    refresh();
                });
                builder.setNegativeButton(R.string.action_cancel, null);
                builder.create().show();
                pd.dismiss();
            }

            @Override
            public void collaboratorsLoadError(APIHandler.APIError error) {
                Toast.makeText(IssuesActivity.this, error.resId, Toast.LENGTH_SHORT).show();
            }
        }, mRepoPath);
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

        final CommentDialog dialog = new CommentDialog();
        dialog.enableNeutralButton();
        dialog.setListener(new CommentDialog.CommentDialogListener() {
            @Override
            public void onPositive(String body) {
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
            public void onCancelled() {
                mRefresher.setRefreshing(true);
                if(issue.isClosed()) {
                    mEditor.openIssue(listener, issue.getRepoPath(), issue.getNumber());
                } else {
                    mEditor.closeIssue(listener, issue.getRepoPath(), issue.getNumber());
                }Toast.makeText(getApplicationContext(), "TODO: Edit issue", Toast.LENGTH_SHORT).show();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_settings:
                startActivity(new Intent(IssuesActivity.this, SettingsActivity.class));
                break;
            case R.id.menu_source:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL)));
                break;
            case R.id.menu_share:
                if(mRepoPath != null) {
                    final Intent share = new Intent();
                    share.setAction(Intent.ACTION_SEND);
                    share.putExtra(Intent.EXTRA_TEXT, "https://github.com/" + mRepoPath + "/issues/");
                    share.setType("text/plain");
                    startActivity(share);
                }
                break;
            case R.id.menu_save_to_homescreen:
                final ShortcutDialog dialog = new ShortcutDialog();
                final Bundle args = new Bundle();
                args.putInt(getString(R.string.intent_title_res), R.string.title_save_issue_shortcut);
                args.putBoolean(getString(R.string.intent_drawable), false);
                args.putString(getString(R.string.intent_name), "Issues");

                dialog.setArguments(args);
                dialog.setListener((name, iconFlag) -> {
                    final Intent i = new Intent(getApplicationContext(), IssuesActivity.class);
                    i.putExtra(getString(R.string.intent_repo), mRepoPath);

                    final Intent add = new Intent();
                    add.putExtra(Intent.EXTRA_SHORTCUT_INTENT, i);
                    add.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
                    add.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.mipmap.ic_launcher));
                    add.putExtra("duplicate", false);
                    add.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                    getApplicationContext().sendBroadcast(add);
                });
                dialog.show(getSupportFragmentManager(), TAG);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }
}
