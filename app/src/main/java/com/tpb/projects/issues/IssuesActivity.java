package com.tpb.projects.issues;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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
import com.tpb.projects.editors.CommentEditor;
import com.tpb.projects.editors.IssueEditor;
import com.tpb.projects.editors.MultiChoiceDialog;
import com.tpb.projects.util.Analytics;
import com.tpb.projects.util.ShortcutDialog;
import com.tpb.projects.util.UI;

import org.sufficientlysecure.htmltextview.HtmlTextView;

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
    private final ArrayList<String> mLabelsFilter = new ArrayList<>();
    private SearchView mSearchView;
    private MenuItem mSearchItem;

    private Repository.AccessLevel mAccessLevel;
    private String mRepoPath;
    private int mPage = 1;
    private boolean mMaxPageReached;
    private boolean mIsLoading;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences.getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        UI.setStatusBarColor(getWindow(), getResources().getColor(R.color.colorPrimaryDark));
        setContentView(R.layout.activity_issues);
        ButterKnife.bind(this);
        mAnalytics = FirebaseAnalytics.getInstance(this);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mLoader = new Loader(this);
        mEditor = new Editor(this);

        mAdapter = new IssuesAdapter(this);

        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecycler.setLayoutManager(layoutManager);
        mRecycler.setAdapter(mAdapter);

        mRefresher.setOnRefreshListener(this::refresh);

        if(getIntent().getExtras() != null && getIntent().getExtras().containsKey(getString(R.string.intent_repo))) {
            mRepoPath = getIntent().getExtras().getString(getString(R.string.intent_repo));
            loadIssues(true);
            mRefresher.setRefreshing(true);

            //Check if we have access to edit the Issue
            mLoader.checkIfCollaborator(new Loader.AccessCheckListener() {
                @Override
                public void accessCheckComplete(Repository.AccessLevel accessLevel) {
                    mAccessLevel = accessLevel;
                    if(mAccessLevel != Repository.AccessLevel.NONE) {
                        mFab.postDelayed(mFab::show, 300);
                        enableScrollListener(mRecycler, layoutManager);
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
                    Log.i(TAG, "onScrolled: Scrolled to bottom");
                    if(!mIsLoading && !mMaxPageReached) {
                        mPage++;
                        mRefresher.setRefreshing(true);
                        loadIssues(false);
                    }
                }
            }
        });
    }

    private void loadIssues(boolean resetPage) {
        mIsLoading = true;
        if(resetPage) {
            mPage = 1;
            mMaxPageReached = false;
        }
        if(mAssigneeFilter == null || mAssigneeFilter.equals(getString(R.string.text_assignee_all))) {
            mLoader.loadIssues(IssuesActivity.this, mRepoPath, mFilter, null, mLabelsFilter, mPage);
        } else if(mAssigneeFilter.equals(getString(R.string.text_assignee_none))) {
            mLoader.loadIssues(IssuesActivity.this, mRepoPath, mFilter, "none", mLabelsFilter, mPage);
        } else {
            mLoader.loadIssues(IssuesActivity.this, mRepoPath, mFilter, mAssigneeFilter, mLabelsFilter, mPage);
        }
    }

    private void refresh() {
        mAdapter.clear();
        loadIssues(true);
        mRefresher.setRefreshing(true);
    }

    @Override
    public void issuesLoaded(Issue[] issues) {
        if(mPage == 1) {
            mAdapter.loadIssues(issues);
        } else {
            if(issues.length > 0) {
                mAdapter.addIssues(issues);
            } else {
                mMaxPageReached = true;
            }
        }
        mIsLoading = false;
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
                builder.setPositiveButton(R.string.action_ok, (dialogInterface, i) -> refresh());
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
                    editIssue(view, issue);
                    break;
            }
            return false;
        });
        menu.show();
    }

    private void editIssue(View view, Issue issue) {
        final Intent intent = new Intent(IssuesActivity.this, IssueEditor.class);
        intent.putExtra(getString(R.string.intent_repo), mRepoPath);
        intent.putExtra(getString(R.string.parcel_issue), issue);
        if(view instanceof HtmlTextView) {
            UI.setClickPositionForIntent(this, intent, ((HtmlTextView) view).getLastClickPosition());
        } else {
            UI.setViewPositionForIntent(intent, view);
        }
        startActivityForResult(intent, IssueEditor.REQUEST_CODE_EDIT_ISSUE);

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

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_state_change_comment);
        builder.setPositiveButton(R.string.action_ok, (dialog, which) -> {
            final Intent i = new Intent(IssuesActivity.this, CommentEditor.class);
            i.putExtra(getString(R.string.parcel_issue), issue);
            startActivityForResult(i, CommentEditor.REQUEST_CODE_COMMENT_FOR_STATE);
            if(issue.isClosed()) {
                mEditor.openIssue(listener, issue.getRepoPath(), issue.getNumber());
            } else {
                mEditor.closeIssue(listener, issue.getRepoPath(), issue.getNumber());
            }

        });
        builder.setNegativeButton(R.string.action_no, (dialog, which) -> {
            if(issue.isClosed()) {
                mEditor.openIssue(listener, issue.getRepoPath(), issue.getNumber());
            } else {
                mEditor.closeIssue(listener, issue.getRepoPath(), issue.getNumber());
            }
        });
        builder.setNeutralButton(R.string.action_cancel, null);
        builder.create().show();

        final Intent i = new Intent(IssuesActivity.this, CommentEditor.class);
        i.putExtra(getString(R.string.parcel_issue), issue);
        startActivityForResult(i, CommentEditor.REQUEST_CODE_COMMENT_FOR_STATE);
    }

    private void moveTo(Issue issue) {
        final int index = mAdapter.indexOf(issue);
        mRecycler.scrollToPosition(index);
        //Wait until the scroll has finished
        new Handler().postDelayed(() ->
            UI.flashViewBackground(
                    mRecycler.findViewHolderForAdapterPosition(index).itemView,
                    getResources().getColor(R.color.md_grey_800),
                    getResources().getColor(R.color.colorAccent))
        , 300);

    }

    @OnClick(R.id.issues_fab)
    public void createNewIssue() {
        final Intent intent = new Intent(IssuesActivity.this, IssueEditor.class);
        intent.putExtra(getString(R.string.intent_repo), mRepoPath);
        startActivityForResult(intent, IssueEditor.REQUEST_CODE_NEW_ISSUE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == IssueEditor.RESULT_OK) {
            final String[] assignees;
            final String[] labels;
            if(data.hasExtra(getString(R.string.intent_issue_assignees))) {
                assignees = data.getStringArrayExtra(getString(R.string.intent_issue_assignees));
            } else {
                assignees = null;
            }
            if(data.hasExtra(getString(R.string.intent_issue_labels))) {
                labels = data.getStringArrayExtra(getString(R.string.intent_issue_labels));
            } else {
                labels = null;
            }
            final Issue issue = data.getParcelableExtra(getString(R.string.parcel_issue));
            if(requestCode == IssueEditor.REQUEST_CODE_NEW_ISSUE) {

                mRefresher.setRefreshing(true);
                mEditor.createIssue(new Editor.IssueCreationListener() {
                    @Override
                    public void issueCreated(Issue issue) {
                        mAdapter.addIssue(issue);
                        mRefresher.setRefreshing(false);
                        final Bundle bundle = new Bundle();
                        bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                        mAnalytics.logEvent(Analytics.TAG_ISSUE_CREATED, bundle);
                    }

                    @Override
                    public void issueCreationError(APIHandler.APIError error) {
                        mRefresher.setRefreshing(false);
                        final Bundle bundle = new Bundle();
                        bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                        mAnalytics.logEvent(Analytics.TAG_ISSUE_CREATED, bundle);
                    }
                }, mRepoPath, issue.getTitle(), issue.getBody(), assignees, labels);
            } else if(requestCode == IssueEditor.REQUEST_CODE_EDIT_ISSUE) {
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
            } else if(requestCode == CommentEditor.REQUEST_CODE_COMMENT_FOR_STATE) {
                final Comment comment = data.getParcelableExtra(getString(R.string.parcel_comment));
                mEditor.createComment(new Editor.CommentCreationListener() {
                    @Override
                    public void commentCreated(Comment comment) {
                        mRefresher.setRefreshing(true);
                    }

                    @Override
                    public void commentCreationError(APIHandler.APIError error) {
                        mRefresher.setRefreshing(false);
                        if(error == APIHandler.APIError.NO_CONNECTION) {
                            Toast.makeText(IssuesActivity.this, error.resId, Toast.LENGTH_SHORT).show();
                        }
                    }
                }, issue.getRepoPath(), issue.getNumber(), comment.getBody());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_search, menu);
        mSearchItem = menu.findItem(R.id.menu_action_search);

        if(mSearchItem != null) {
            mSearchView = (SearchView) mSearchItem.getActionView();
        }

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
            case R.id.menu_action_search:
                if(mAdapter.getItemCount() > 0) {
                    final SearchView.SearchAutoComplete searchSrc = (SearchView.SearchAutoComplete) mSearchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
                    searchSrc.setThreshold(1);
                    final IssuesSearchAdapter searchAdapter = new IssuesSearchAdapter(this, mAdapter.getIssues());
                    searchSrc.setAdapter(searchAdapter);
                    searchSrc.setOnItemClickListener((adapterView, view, i, l) -> {
                        mSearchItem.collapseActionView();
                        moveTo(searchAdapter.getItem(i));
                    });
                }
        }
        return super.onOptionsItemSelected(item);
    }

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }
}
