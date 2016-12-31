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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
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
import com.tpb.projects.data.Editor;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.Card;
import com.tpb.projects.data.models.Column;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Project;
import com.tpb.projects.project.dialogs.CardDialog;
import com.tpb.projects.project.dialogs.NewIssueDialog;
import com.tpb.projects.user.SettingsActivity;
import com.tpb.projects.util.Analytics;

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
    private ShareActionProvider mShareActionProvider;

    @BindView(R.id.project_toolbar) Toolbar mToolbar;
    @BindView(R.id.project_name) TextView mName;
    @BindView(R.id.project_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.project_column_pager) ViewPager mColumnPager;
    @BindView(R.id.project_fab_menu) FloatingActionMenu mMenu;
    @BindView(R.id.project_add_card) FloatingActionButton mAddCard;
    @BindView(R.id.project_add_column) FloatingActionButton mAddColumn;
    @BindView(R.id.project_add_issue) FloatingActionButton mAddIssue;

    private ColumnPagerAdapter mAdapter;
    private int mCurrentPosition = -1;
    private Loader mLoader;
    Project mProject;
    private Editor mEditor;
    private NavigationDragListener mNavListener;
    private boolean mCanEdit;

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
        }
        mCanEdit = launchIntent.getBooleanExtra(getString(R.string.intent_can_edit), false);
        if(!mCanEdit) mMenu.hideMenu(false);
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
                showFab();
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

        mRefresher.setOnRefreshListener(() -> mLoader.loadProject(ProjectActivity.this, mProject.getId()));
        mNavListener = new NavigationDragListener();
        mRefresher.setOnDragListener(mNavListener);

        new Handler().postDelayed(() -> mMenu.showMenuButton(true), 400);

        mLoader.loadLabels(null, mProject.getRepoFullName());

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
        mName.setText(project.getName());

        final Bundle bundle = new Bundle();
        bundle.putString(Analytics.KEY_LOAD_STATUS, Analytics.VALUE_SUCCESS);
        mAnalytics.logEvent(Analytics.TAG_PROJECT_LOADED, bundle);

        mLoader.loadColumns(new Loader.ColumnsLoader() {
            @Override
            public void columnsLoaded(Column[] columns) {
                mRefresher.setRefreshing(false);
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
            public void columnsLoadError() {
                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_LOAD_STATUS, Analytics.VALUE_FAILURE);
                mAnalytics.logEvent(Analytics.TAG_COLUMNS_LOADED, bundle);
            }
        }, project.getId());
    }

    @Override
    public void projectLoadError() {
        final Bundle bundle = new Bundle();
        bundle.putString(Analytics.KEY_LOAD_STATUS, Analytics.VALUE_FAILURE);
        mAnalytics.logEvent(Analytics.TAG_PROJECT_LOADED, bundle);
    }

    void loadIssue(Loader.IssueLoader loader, int issueId) {
        mLoader.loadIssue(loader, mProject.getRepoFullName(), issueId);
    }

    @OnClick(R.id.project_add_column)
    void addColumn() {
        mMenu.close(true);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(R.layout.dialog_new_column)
                .setTitle(R.string.title_new_column)
                .setNegativeButton(R.string.action_cancel, null)
                .create();
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.action_ok), (di, w) -> {}); //Null is ambiguous so we pass empty lambda
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
                    public void columnAdditionError() {
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
        final NewIssueDialog newDialog = new NewIssueDialog();
        newDialog.setListener(new NewIssueDialog.IssueDialogListener() {
            @Override
            public void issueCreated(Issue issue) {
                mAdapter.getCurrentFragment().createIssueCard(issue);

            }

            @Override
            public void issueCreationCancelled() {

            }
        });
        final Bundle c = new Bundle();
        c.putString(getString(R.string.intent_repo), mProject.getRepoFullName());
        newDialog.setArguments(c);
        newDialog.show(getSupportFragmentManager(), TAG);
    }

    void deleteColumn(Column column) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_delete_column)
                .setMessage(R.string.text_delete_column_warning)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_ok, (dialogInterface, i) -> {
                    mRefresher.setRefreshing(true);
                    mEditor.deleteColumn(new Editor.ColumnDeletionListener() {
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
                        public void columnDeletionError() {
                            final Bundle bundle = new Bundle();
                            bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                            mAnalytics.logEvent(Analytics.TAG_COLUMN_DELETE, bundle);
                        }
                    }, column.getId());
                }).show();
    }

    /**
     *
     * @param tag id of the column being moved
     * @param dropTag id of the column being dropped onto
     * @param direction side of the drop column to drop to true=left false=right
     */
    void moveColumn(int tag, int dropTag, boolean direction) {
        final int from = mAdapter.indexOf(tag);
        final int to;
        if(direction) {
            Log.i(TAG, "moveColumn: Dropping to the left");
            to = Math.max(0, mAdapter.indexOf(dropTag) - 1);
        } else {
            to = Math.min(mAdapter.getCount() - 1,  mAdapter.indexOf(dropTag) + 1);
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
            public void columnMovementError() {

            }
        }, tag, dropTag, to);
    }

    @OnClick(R.id.project_add_card)
    void addCard() {
        mMenu.close(true);
        final CardDialog dialog = new CardDialog();
        final ArrayList<Integer> ids = new ArrayList<>();
        for(int i = 0; i < mAdapter.getCount(); i++) {
            for(Card c : mAdapter.getExistingFragment(i).getCards()) {
                if(c.hasIssue()) ids.add(c.getIssue().getId());
            }
        }
        final Bundle bundle = new Bundle();
        bundle.putString(getString(R.string.intent_repo), mProject.getRepoFullName());
        bundle.putIntegerArrayList(getString(R.string.intent_int_arraylist), ids);
        dialog.setArguments(bundle);
        mAdapter.getCurrentFragment().showCardDialog(dialog);
    }

    void deleteCard(Card card, boolean showWarning) {
        final Editor.CardDeletionListener listener = new Editor.CardDeletionListener() {
            @Override
            public void cardDeleted(Card card) {
                mRefresher.setRefreshing(false);
                mAdapter.getCurrentFragment().removeCard(card);
                Snackbar.make(findViewById(R.id.project_coordinator),
                        getString(R.string.text_note_deleted), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.action_undo), view ->  mAdapter.getCurrentFragment().recreateCard(card))
                        .show();
            }

            @Override
            public void cardDeletionError() {

            }
        };
        if(showWarning) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.title_delete_card)
                    .setMessage(R.string.text_delete_note_warning)
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(R.string.action_ok, (dialogInterface, i) -> {
                        mRefresher.setRefreshing(true);
                        mEditor.deleteCard(listener, card);
                    }).show();
        } else {
            mRefresher.setRefreshing(true);
            mEditor.deleteCard(listener, card);
        }
    }

    private long lastPageChange;

    private void dragLeft() {
        if(mCurrentPosition > 0 && System.nanoTime() - lastPageChange > 5E8) {
            mColumnPager.setCurrentItem(mCurrentPosition - 1, true);
            lastPageChange = System.nanoTime();
        }
    }

    private void dragRight() {
        if(mCurrentPosition < mAdapter.getCount() && System.nanoTime() - lastPageChange > 5E8) {
            mColumnPager.setCurrentItem(mCurrentPosition + 1, true);
            lastPageChange = System.nanoTime();
        }
    }

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        if(mMenu.isOpened()) {
            mMenu.close(true);
        } else {
            if(mAdapter.getCurrentFragment() != null) mAdapter.getCurrentFragment().hideRecycler();
            mMenu.hideMenuButton(true);
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity, menu);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menu.findItem(R.id.menu_share));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_settings) {
            startActivity(new Intent(ProjectActivity.this, SettingsActivity.class));
        } else if(item.getItemId() == R.id.menu_source) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL)));
        } else if(item.getItemId() == R.id.menu_share) {
            mShareActionProvider.setShareIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/" + mProject.getRepoFullName() + "/projects/" + Integer.toString(mProject.getNumber()))));
        }

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAnalytics.setAnalyticsCollectionEnabled(SettingsActivity.Preferences.getPreferences(this).areAnalyticsEnabled());
    }

    class NavigationDragListener implements View.OnDragListener {

        @Override
        public boolean onDrag(View view, DragEvent event) {

            if(event.getAction() == DragEvent.ACTION_DRAG_LOCATION) {
                final DisplayMetrics metrics = getResources().getDisplayMetrics();

                if(event.getX() / metrics.widthPixels > 0.85f) {
                    dragRight();
                } else if(event.getX() / metrics.widthPixels < 0.15f) {
                    dragLeft();
                }
            }
            switch(event.getAction()) {
                case DragEvent.ACTION_DRAG_EXITED:
                    Log.i(TAG, "onDrag: Exited");
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    Log.i(TAG, "onDrag: Ended");
                    break;
                case DragEvent.ACTION_DROP:
                    Log.i(TAG, "onDrag: Dropped");
                    break;
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

        @Override
        protected ColumnFragment createFragment(PageDescriptor pageDescriptor) {
            return ColumnFragment.getInstance(((ColumnPageDescriptor) pageDescriptor).mColumn,
                    mNavListener,
                    mCanEdit,
                    columns.indexOf(((ColumnPageDescriptor) pageDescriptor).mColumn) == mCurrentPosition);
        }


    }

    private static class ColumnPageDescriptor implements PageDescriptor {
        private Column mColumn;

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
