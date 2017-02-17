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

package com.tpb.projects.project;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.pager.PageDescriptor;
import com.commonsware.cwac.pager.v4.ArrayPagerAdapter;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Editor;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.auth.GitHubSession;
import com.tpb.projects.data.models.Card;
import com.tpb.projects.data.models.Column;
import com.tpb.projects.data.models.Comment;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Project;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.editors.CardEditor;
import com.tpb.projects.editors.CommentEditor;
import com.tpb.projects.editors.IssueEditor;
import com.tpb.projects.util.Analytics;
import com.tpb.projects.util.ShortcutDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


/**
 * Created by theo on 19/12/16.
 */

public class ProjectActivity extends AppCompatActivity implements Loader.ProjectLoader {
    private static final String TAG = ProjectActivity.class.getSimpleName();
    private static final String URL = "https://github.com/tpb1908/AndroidProjectsClient/blob/master/app/src/main/java/com/tpb/projects/project/ProjectActivity.java";

    private FirebaseAnalytics mAnalytics;

    @BindView(R.id.project_toolbar) Toolbar mToolbar;
    @BindView(R.id.project_name) TextView mName;
    @BindView(R.id.project_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.project_column_pager) ViewPager mColumnPager;
    @BindView(R.id.project_fab_menu) FloatingActionMenu mMenu;
    @BindView(R.id.project_add_card) FloatingActionButton mAddCard;
    @BindView(R.id.project_add_column) FloatingActionButton mAddColumn;
    @BindView(R.id.project_add_issue) FloatingActionButton mAddIssue;
    private SearchView mSearchView;
    private MenuItem mSearchItem;

    private ColumnPagerAdapter mAdapter;
    private int mCurrentPosition = -1;
    private Loader mLoader;
    Project mProject;
    private Editor mEditor;
    private NavigationDragListener mNavListener;
    private Repository.AccessLevel mAccessLevel = Repository.AccessLevel.NONE;
    private int mLaunchCardId = -1;
    private int mLoadCount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences.getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        setContentView(R.layout.activity_project);
        ButterKnife.bind(this);

        mAnalytics = FirebaseAnalytics.getInstance(this);
        mAnalytics.setAnalyticsCollectionEnabled(prefs.areAnalyticsEnabled());
        final Intent launchIntent = getIntent();
        mLoader = new Loader(this);
        mEditor = new Editor(this);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if(launchIntent.hasExtra(getString(R.string.parcel_project))) {
            projectLoaded(launchIntent.getParcelableExtra(getString(R.string.parcel_project)));
            mAccessLevel = (Repository.AccessLevel) launchIntent.getSerializableExtra(getString(R.string.intent_access_level));
            if(mAccessLevel == Repository.AccessLevel.ADMIN || mAccessLevel == Repository.AccessLevel.WRITE) {
                new Handler().postDelayed(() -> mMenu.showMenuButton(true), 400);
            }
        } else {
            final String repo = launchIntent.getStringExtra(getString(R.string.intent_repo));
            final int number = launchIntent.getIntExtra(getString(R.string.intent_project_number), 1);
            if(launchIntent.hasExtra(getString(R.string.intent_card_id))) {
                mLaunchCardId = launchIntent.getIntExtra(getString(R.string.intent_card_id), -1);
            }
            loadFromId(repo, number);
        }
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        mAdapter = new ColumnPagerAdapter(getSupportFragmentManager(), new ArrayList<>());
        mColumnPager.setAdapter(mAdapter);
        mColumnPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mCurrentPosition = position;
                Log.i(TAG, "onPageSelected: Page changed to  " + position);
                if(mAccessLevel == Repository.AccessLevel.ADMIN || mAccessLevel == Repository.AccessLevel.WRITE) {
                    showFab();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if(state == ViewPager.SCROLL_STATE_DRAGGING) {
                    mRefresher.setEnabled(false);
                } else if(state == ViewPager.SCROLL_STATE_IDLE) {
                    mRefresher.setEnabled(true);
                }
            }
        });
        mRefresher.setRefreshing(true);
        mMenu.hideMenuButton(false); //Hide the button so that we can show it later
        mMenu.setClosedOnTouchOutside(true);
        mRefresher.setOnRefreshListener(() -> {
            if(mProject != null) {
                mLoader.loadProject(ProjectActivity.this, mProject.getId());
            } else {
                final String repo = launchIntent.getStringExtra(getString(R.string.intent_repo));
                final int number = launchIntent.getIntExtra(getString(R.string.intent_project_number), 1);
                loadFromId(repo, number);
            }
        });
        mNavListener = new NavigationDragListener();
        mRefresher.setOnDragListener(mNavListener);
    }

    private void loadFromId(String repo, int number) {
        //We have to load all of the projects to get the id that we want
        mLoader.loadProjects(new Loader.ProjectsLoader() {
            int projectLoadAttempts = 0;
            @Override
            public void projectsLoaded(Project[] projects) {
                for(Project p : projects) {
                    if(number == p.getNumber()) {
                        projectLoaded(p);
                        checkAccess(p);
                        return;
                    }
                }
                Toast.makeText(ProjectActivity.this, R.string.error_project_not_found, Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void projectsLoadError(APIHandler.APIError error) {
                if(error == APIHandler.APIError.NO_CONNECTION) {
                    mRefresher.setRefreshing(false);
                    Toast.makeText(ProjectActivity.this, error.resId, Toast.LENGTH_SHORT).show();

                } else {
                    if(projectLoadAttempts < 5) {
                        projectLoadAttempts++;
                        mLoader.loadProjects(this, repo);
                    } else {
                        Toast.makeText(ProjectActivity.this, error.resId, Toast.LENGTH_SHORT).show();
                        mRefresher.setRefreshing(false);
                    }
                }
            }
        }, repo);
    }
    private void checkAccess(Project project) {
        mLoader.checkAccessToRepository(new Loader.AccessCheckListener() {
            int accessCheckAttempts = 0;
            @Override
            public void accessCheckComplete(Repository.AccessLevel accessLevel) {
                Log.i(TAG, "accessCheckComplete: " + accessLevel);
                mAccessLevel = accessLevel;
                if(mAccessLevel == Repository.AccessLevel.ADMIN || mAccessLevel == Repository.AccessLevel.WRITE) {
                    mMenu.showMenuButton(true);
                } else {
                    mMenu.hideMenuButton(false);
                }
                for(int i = 0; i < mAdapter.getCount(); i++) {
                    if(mAdapter.getExistingFragment(i) != null) {
                        mAdapter.getExistingFragment(i).setAccessLevel(mAccessLevel);
                    }
                }
            }

            @Override
            public void accessCheckError(APIHandler.APIError error) {
                if(error == APIHandler.APIError.NO_CONNECTION) {
                    mRefresher.setRefreshing(false);
                    Toast.makeText(ProjectActivity.this, error.resId, Toast.LENGTH_SHORT).show();

                } else {
                    if(accessCheckAttempts < 5) {
                        accessCheckAttempts++;
                        mLoader.checkAccessToRepository(this, GitHubSession.getSession(ProjectActivity.this).getUserLogin(), project.getRepoPath());
                    } else {
                        Toast.makeText(ProjectActivity.this, error.resId, Toast.LENGTH_SHORT).show();
                        mRefresher.setRefreshing(false);
                    }
                }
            }
        }, GitHubSession.getSession(this).getUserLogin(), project.getRepoPath());
    }

    void showFab() {
        mMenu.showMenuButton(true);
    }

    void hideFab() {
        mMenu.hideMenuButton(true);
    }

    @Override
    public void projectLoaded(Project project) {
        Log.i(TAG, "projectLoaded: Owner url " + project.getOwnerUrl());
        mProject = project;
        mLoader.loadLabels(null, mProject.getRepoPath());
        mName.setText(project.getName());

        final Bundle bundle = new Bundle();
        bundle.putString(Analytics.KEY_LOAD_STATUS, Analytics.VALUE_SUCCESS);
        mAnalytics.logEvent(Analytics.TAG_PROJECT_LOADED, bundle);
        mLoadCount = 0;
        mLoader.loadColumns(new Loader.ColumnsLoader() {
            @Override
            public void columnsLoaded(Column[] columns) {
                if(columns.length > 0) {
                    mAddCard.setVisibility(View.INVISIBLE);
                    mAddIssue.setVisibility(View.INVISIBLE);

                    int id = 0;
                    if(mCurrentPosition != -1) {
                        id = mAdapter.getCurrentFragment().mColumn.getId();
                    }
                    mCurrentPosition = 0;
                    mAdapter.columns = new ArrayList<>(Arrays.asList(columns));
                    if(mAdapter.getCount() != 0) {
                        for(int i = mAdapter.getCount() - 1; i >= 0; i--) mAdapter.remove(i);
                    }
                    for(int i = 0; i < columns.length; i++) {
                        mAdapter.add(new ColumnPageDescriptor(columns[i]));
                        if(columns[i].getId() == id) {
                            mCurrentPosition = i;
                        }
                    }
                    mColumnPager.setOffscreenPageLimit(mAdapter.getCount());
                    if(mCurrentPosition >= mAdapter.getCount()) {
                        mCurrentPosition = mAdapter.getCount() - 1;
                        //If the end column has been deleted
                    }
                    mColumnPager.setCurrentItem(mCurrentPosition, true);
                    mColumnPager.postDelayed(() -> mColumnPager.setVisibility(View.VISIBLE), 300);
                } else {
                    mAddCard.setVisibility(View.GONE);
                    mAddIssue.setVisibility(View.GONE);
                }

                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_LOAD_STATUS, Analytics.VALUE_SUCCESS);
                bundle.putInt(Analytics.KEY_COLUMN_COUNT, columns.length + 1);
                mAnalytics.logEvent(Analytics.TAG_COLUMNS_LOADED, bundle);
            }

            @Override
            public void columnsLoadError(APIHandler.APIError error) {
                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_LOAD_STATUS, Analytics.VALUE_FAILURE);
                mAnalytics.logEvent(Analytics.TAG_COLUMNS_LOADED, bundle);
            }
        }, project.getId());
    }

    @Override
    public void projectLoadError(APIHandler.APIError error) {
        final Bundle bundle = new Bundle();
        bundle.putString(Analytics.KEY_LOAD_STATUS, Analytics.VALUE_FAILURE);
        mAnalytics.logEvent(Analytics.TAG_PROJECT_LOADED, bundle);
    }

    void loadIssue(Loader.IssueLoader loader, int issueId, Column column) {
        mLoader.loadIssue(loader, mProject.getRepoPath(), issueId, mAdapter.indexOf(column.getId()) == mCurrentPosition);
    }

    @OnClick(R.id.project_add_column)
    void addColumn() {
        mMenu.close(true);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(R.layout.dialog_new_column)
                .setTitle(R.string.title_new_column)
                .setNegativeButton(R.string.action_cancel, null)
                .create();
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.action_ok), (di, w) -> {
        }); //Null is ambiguous so we pass empty lambda
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            final EditText editor = (EditText) dialog.findViewById(R.id.project_new_column);
            final String text = editor.getText().toString();
            final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if(imm.isActive()) {
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            }
            if(!text.isEmpty()) {
                mRefresher.setRefreshing(true);
                mEditor.addColumn(new Editor.ColumnAdditionListener() {
                    int addColumnAttempts = 0;
                    @Override
                    public void columnAdded(Column column) {
                        mAddCard.setVisibility(View.INVISIBLE);
                        mAddIssue.setVisibility(View.INVISIBLE);
                        mAdapter.columns.add(column);
                        mAdapter.add(new ColumnPageDescriptor(column));
                        mColumnPager.setCurrentItem(mAdapter.getCount(), true);
                        mRefresher.setRefreshing(false);
                        final Bundle bundle = new Bundle();
                        bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                        mAnalytics.logEvent(Analytics.TAG_COLUMN_ADD, bundle);
                    }

                    @Override
                    public void columnAdditionError(APIHandler.APIError error) {
                        if(error == APIHandler.APIError.NO_CONNECTION) {
                            mRefresher.setRefreshing(false);
                            Toast.makeText(ProjectActivity.this, error.resId, Toast.LENGTH_SHORT).show();
        
                        } else {
                            if(addColumnAttempts < 5) {
                                addColumnAttempts++;
                               mEditor.addColumn(this, mProject.getId(), text);
                            } else {
                                Toast.makeText(ProjectActivity.this, error.resId, Toast.LENGTH_SHORT).show();
                                mRefresher.setRefreshing(false);
                            }
                        }
                        final Bundle bundle = new Bundle();
                        bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                        mAnalytics.logEvent(Analytics.TAG_COLUMN_ADD, bundle);
                    }
                }, mProject.getId(), text);
                dialog.dismiss();
            } else {
                Toast.makeText(this, R.string.error_no_column_title, Toast.LENGTH_SHORT).show();
            }
        });
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if(imm.isActive()) {
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            }
            dialog.dismiss();
        });
        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    @OnClick(R.id.project_add_issue)
    void addIssue() {
        mMenu.close(true);

        final Intent intent = new Intent(ProjectActivity.this, IssueEditor.class);
        intent.putExtra(getString(R.string.intent_repo), mProject.getRepoPath());
        startActivityForResult(intent, IssueEditor.REQUEST_CODE_NEW_ISSUE);
    }

    void deleteColumn(Column column) {
        new AlertDialog.Builder(this, R.style.DialogAnimation)
                .setTitle(R.string.title_delete_column)
                .setMessage(R.string.text_delete_column_warning)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_ok, (dialogInterface, i) -> {
                    mRefresher.setRefreshing(true);
                    mEditor.deleteColumn(new Editor.ColumnDeletionListener() {
                        int deleteColumnAttempts = 0;
                        @Override
                        public void columnDeleted() {
                            mAdapter.remove(mCurrentPosition);
                            mAdapter.columns.remove(mCurrentPosition);
                            mRefresher.setRefreshing(false);
                            if(mAdapter.columns.size() == 0) {
                                mAddCard.setVisibility(View.GONE);
                                mAddIssue.setVisibility(View.GONE);
                            }
                            final Bundle bundle = new Bundle();
                            bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                            mAnalytics.logEvent(Analytics.TAG_COLUMN_DELETE, bundle);
                        }

                        @Override
                        public void columnDeletionError(APIHandler.APIError error) {
                            if(error == APIHandler.APIError.NO_CONNECTION) {
                                mRefresher.setRefreshing(false);
                                Toast.makeText(ProjectActivity.this, error.resId, Toast.LENGTH_SHORT).show();
            
                            } else {
                                if(deleteColumnAttempts < 5) {
                                    deleteColumnAttempts++;
                                    mEditor.deleteColumn(this, column.getId());
                                } else {
                                    Toast.makeText(ProjectActivity.this, error.resId, Toast.LENGTH_SHORT).show();
                                    mRefresher.setRefreshing(false);
                                }
                            }
                            final Bundle bundle = new Bundle();
                            bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                            mAnalytics.logEvent(Analytics.TAG_COLUMN_DELETE, bundle);
                        }
                    }, column.getId());
                }).show();

    }

    /**
     * @param tag       id of the column being moved
     * @param dropTag   id of the column being dropped onto
     * @param direction side of the drop column to drop to true=left false=right
     */
    void moveColumn(int tag, int dropTag, boolean direction) {
        final int from = mAdapter.indexOf(tag);
        final int to;
        if(direction) {
            Log.i(TAG, "moveColumn: Dropping to the left");
            to = Math.max(0, mAdapter.indexOf(dropTag) - 1);
        } else {
            to = Math.min(mAdapter.getCount() - 1, mAdapter.indexOf(dropTag) + 1);
        }
        Log.i(TAG, "moveColumn: From " + from + ", to " + to);
        mAdapter.move(from, to);
        mAdapter.columns.add(to, mAdapter.columns.remove(from));
        mColumnPager.setCurrentItem(to, true);
        mEditor.moveColumn(new Editor.ColumnMovementListener() {
            @Override
            public void columnMoved(int columnId) {

            }

            @Override
            public void columnMovementError(APIHandler.APIError error) {

            }
        }, tag, dropTag, to);
    }

    @OnClick(R.id.project_add_card)
    void addCard() {
        mMenu.close(true);
        final Intent intent = new Intent(this, CardEditor.class);

        final ArrayList<Integer> ids = new ArrayList<>();
        for(int i = 0; i < mAdapter.getCount(); i++) {
            for(Card c : mAdapter.getExistingFragment(i).getCards()) {
                if(c.hasIssue()) ids.add(c.getIssue().getId());
            }
        }
        intent.putExtra(getString(R.string.intent_repo), mProject.getRepoPath());
        intent.putIntegerArrayListExtra(getString(R.string.intent_int_arraylist), ids);
        startActivityForResult(intent, CardEditor.REQUEST_CODE_NEW_CARD);
        //mAdapter.getCurrentFragment().showCardDialog(dialog);
    }

    void deleteCard(Card card, boolean showWarning) {
        final Editor.CardDeletionListener listener = new Editor.CardDeletionListener() {
            @Override
            public void cardDeleted(Card card) {
                mRefresher.setRefreshing(false);
                mAdapter.getCurrentFragment().removeCard(card);
                Snackbar.make(findViewById(R.id.project_coordinator),
                        getString(R.string.text_note_deleted), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.action_undo), view -> mAdapter.getCurrentFragment().recreateCard(card))
                        .show();
            }

            @Override
            public void cardDeletionError(APIHandler.APIError error) {

            }
        };
        if(showWarning) {
            final Dialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.title_delete_card)
                    .setMessage(R.string.text_delete_note_warning)
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(R.string.action_ok, (dialogInterface, i) -> {
                        mRefresher.setRefreshing(true);
                        mEditor.deleteCard(listener, card);
                    }).create();
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
            dialog.show();
        } else {
            mRefresher.setRefreshing(true);
            mEditor.deleteCard(listener, card);
        }
    }

    private void dragLeft() {
        if(mCurrentPosition > 0) {
            mColumnPager.setCurrentItem(mCurrentPosition - 1, true);
        }
    }

    private void dragRight() {
        if(mCurrentPosition < mAdapter.getCount()) {
            mColumnPager.setCurrentItem(mCurrentPosition + 1, true);
        }
    }

    private void dragUp() {
        mAdapter.getCurrentFragment().scrollUp();
    }

    private void dragDown() {
        mAdapter.getCurrentFragment().scrollDown();
    }

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }

    void notifyFragmentLoaded() {
        mLoadCount++;
        if(mLoadCount == mAdapter.getCount()) {
            mRefresher.setRefreshing(false);
            if(mLaunchCardId != -1) {
                new Handler().postDelayed(() -> mAdapter.moveTo(mLaunchCardId), 500);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if(mMenu.isOpened()) {
            mMenu.close(true);
        } else {
            /*
            This seems to fix the problem with RecyclerView view detaching
            Quick and dirty way of removing the views
             */
            mColumnPager.setAdapter(new ColumnPagerAdapter(getSupportFragmentManager(), new ArrayList<>()));
            mMenu.hideMenuButton(true);
            super.onBackPressed();
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
                startActivity(new Intent(ProjectActivity.this, SettingsActivity.class));
                break;
            case R.id.menu_source:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL)));
                break;
            case R.id.menu_share:
                final Intent share = new Intent();
                share.setAction(Intent.ACTION_SEND);
                share.putExtra(Intent.EXTRA_TEXT, "https://github.com/" +
                        mProject.getRepoPath() +
                        "/projects/" +
                        Integer.toString(mProject.getNumber()));
                share.setType("text/plain");
                startActivity(share);
                break;
            case R.id.menu_save_to_homescreen:
                final ShortcutDialog dialog = new ShortcutDialog();
                final Bundle args = new Bundle();
                args.putInt(getString(R.string.intent_title_res), R.string.title_save_project_shortcut);
                args.putString(getString(R.string.intent_name), mProject.getName());

                dialog.setArguments(args);
                dialog.setListener((name, iconFlag) -> {
                    final Intent i = new Intent(getApplicationContext(), ProjectActivity.class);
                    i.putExtra(getString(R.string.intent_repo), mProject.getRepoPath());
                    i.putExtra(getString(R.string.intent_project_number), mProject.getNumber());

                    final Intent add = new Intent();
                    add.putExtra(Intent.EXTRA_SHORTCUT_INTENT, i);
                    add.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
                    add.putExtra("duplicate", false);
                    add.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.mipmap.ic_launcher));
                    add.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                    getApplicationContext().sendBroadcast(add);
                });
                dialog.show(getSupportFragmentManager(), TAG);
                break;
            case R.id.menu_action_search:
                if(mAdapter.getCount() > 0) {
                    final SearchView.SearchAutoComplete searchSrc = (SearchView.SearchAutoComplete) mSearchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
                    searchSrc.setThreshold(1);
                    final ProjectSearchAdapter searchAdapter = new ProjectSearchAdapter(this, mAdapter.getAllCards());
                    searchSrc.setAdapter(searchAdapter);
                    searchSrc.setOnItemClickListener((adapterView, view, i, l) -> {
                        mSearchItem.collapseActionView();
                        mAdapter.moveTo(searchAdapter.getItem(i).getId());
                    });
                }
        }


        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == AppCompatActivity.RESULT_OK) {
            if(requestCode == IssueEditor.REQUEST_CODE_NEW_ISSUE) {
                String[] assignees = null;
                String[] labels = null;
                if(data.hasExtra(getString(R.string.intent_issue_assignees))) {
                    assignees = data.getStringArrayExtra(getString(R.string.intent_issue_assignees));
                }
                if(data.hasExtra(getString(R.string.intent_issue_labels))) {
                    labels = data.getStringArrayExtra(getString(R.string.intent_issue_labels));
                }
                final Issue issue = data.getParcelableExtra(getString(R.string.parcel_issue));
                mEditor.createIssue(new Editor.IssueCreationListener() {
                    @Override
                    public void issueCreated(Issue issue) {
                        mAdapter.getCurrentFragment().createIssueCard(issue);
                        final Bundle bundle = new Bundle();
                        bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                        mAnalytics.logEvent(Analytics.TAG_ISSUE_CREATED, bundle);
                    }

                    @Override
                    public void issueCreationError(APIHandler.APIError error) {
                        final Bundle bundle = new Bundle();
                        bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                        mAnalytics.logEvent(Analytics.TAG_ISSUE_CREATED, bundle);
                    }
                }, mProject.getRepoPath(), issue.getTitle(), issue.getBody(), assignees, labels);

            } else if(requestCode == CardEditor.REQUEST_CODE_NEW_CARD) {
                final Card card = data.getParcelableExtra(getString(R.string.parcel_card));
                if(card.hasIssue()) {
                    mAdapter.getCurrentFragment().createIssueCard(card.getIssue());
                } else {
                    mAdapter.getCurrentFragment().newCard(card);
                }
            } else if(requestCode == CardEditor.REQUEST_CODE_EDIT_CARD) {
                mAdapter.getCurrentFragment().editCard(data.getParcelableExtra(getString(R.string.parcel_card)));
            } else if(requestCode == CommentEditor.REQUEST_CODE_COMMENT_FOR_STATE) {
                final Comment comment = data.getParcelableExtra(getString(R.string.parcel_comment));
                final Issue issue = data.getParcelableExtra(getString(R.string.parcel_issue));
                mEditor.createComment(new Editor.CommentCreationListener() {
                    @Override
                    public void commentCreated(Comment comment) {
                        Toast.makeText(ProjectActivity.this, R.string.text_comment_created, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void commentCreationError(APIHandler.APIError error) {

                    }
                }, issue.getRepoPath(), issue.getNumber(), comment.getBody());
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAnalytics.setAnalyticsCollectionEnabled(SettingsActivity.Preferences.getPreferences(this).areAnalyticsEnabled());
    }

    class NavigationDragListener implements View.OnDragListener {

        private long mLastPageChange = 0;

        @Override
        public boolean onDrag(View view, DragEvent event) {
            if(event.getAction() == DragEvent.ACTION_DRAG_ENTERED && view.getId() == R.id.viewholder_card) {
                final RecyclerView rv = (RecyclerView) view.getParent();
                final CardAdapter ca = (CardAdapter) rv.getAdapter();
                final Rect r = new Rect();
                ((NestedScrollView) rv.getParent().getParent()).getHitRect(r);
                int first = -1;
                int last = -1;
                for(int i = 0; i < rv.getAdapter().getItemCount(); i++) {
                    if(rv.getChildAt(i).getLocalVisibleRect(r)) {
                        if(first == -1) {
                            first = i;
                        }
                        Log.i(TAG, "onDrag: Position " + i + " onscreen");
                    } else if(first != -1) {
                        last = i - 1;
                        break;
                    }
                }
                Log.i(TAG, "onDrag: Range of positions " + first + ", to " + last);
                final int tp = ca.indexOf((int) view.getTag());
                final LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                int fp = lm.findFirstCompletelyVisibleItemPosition();
                int lp = lm.findLastCompletelyVisibleItemPosition();
                Log.i(TAG, "onDrag: Hovering over position " + tp + " between " + fp + " and " + lp);
            } else if(event.getAction() == DragEvent.ACTION_DRAG_LOCATION) {
                final DisplayMetrics metrics = getResources().getDisplayMetrics();
                if(event.getX() / metrics.widthPixels > 0.85f && System.nanoTime() - mLastPageChange > 5E8) {
                    dragRight();
                    mLastPageChange = System.nanoTime();
                } else if(event.getX() / metrics.widthPixels < 0.15f && System.nanoTime() - mLastPageChange > 5E8) {
                    dragLeft();
                    mLastPageChange = System.nanoTime();
                }
            }
            return true;
        }
    }

    private class ColumnPagerAdapter extends ArrayPagerAdapter<ColumnFragment> {
        private ArrayList<Column> columns = new ArrayList<>();

        ColumnPagerAdapter(FragmentManager manager, List<PageDescriptor> descriptors) {
            super(manager, descriptors);
        }

        int indexOf(int id) {
            for(int i = 0; i < columns.size(); i++) {
                if(columns.get(i).getId() == id) return i;
            }
            return -1;
        }

        ArrayList<Card> getAllCards() {
            final ArrayList<Card> cards = new ArrayList<>();
            for(int i = 0; i < getCount(); i++) {
                cards.addAll(getExistingFragment(i).getCards());
            }
            return cards;
        }

        void moveTo(int cardId) {
            for(int i = 0; i < getCount(); i++) {
                if(getExistingFragment(i).attemptMoveTo(cardId)) {
                    mColumnPager.setCurrentItem(i, true);
                    break;
                }
            }
        }

        @Override
        protected ColumnFragment createFragment(PageDescriptor pageDescriptor) {
            return ColumnFragment.getInstance(((ColumnPageDescriptor) pageDescriptor).mColumn,
                    mNavListener,
                    mAccessLevel,
                    columns.indexOf(((ColumnPageDescriptor) pageDescriptor).mColumn) == mCurrentPosition);
        }

    }

    private static class ColumnPageDescriptor implements PageDescriptor {
        private final Column mColumn;

        ColumnPageDescriptor(Column column) {
            mColumn = column;
        }

        @Override
        public String getFragmentTag() {
            return Integer.toString(mColumn.getId());
        }

        @Override
        public String getTitle() {
            return mColumn.getName();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(mColumn, flags);
        }

        ColumnPageDescriptor(Parcel in) {
            this.mColumn = in.readParcelable(Column.class.getClassLoader());
        }

        public static final Creator<ColumnPageDescriptor> CREATOR = new Creator<ColumnPageDescriptor>() {
            @Override
            public ColumnPageDescriptor createFromParcel(Parcel source) {
                return new ColumnPageDescriptor(source);
            }

            @Override
            public ColumnPageDescriptor[] newArray(int size) {
                return new ColumnPageDescriptor[size];
            }
        };
    }
}
