package com.tpb.projects.project;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
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
import com.tpb.projects.data.models.Project;
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

    @BindView(R.id.project_toolbar) Toolbar mToolbar;
    @BindView(R.id.project_name) TextView mName;
    @BindView(R.id.project_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.project_column_pager) ViewPager mColumnPager;
    @BindView(R.id.project_fab_menu) FloatingActionMenu mMenu;
    @BindView(R.id.project_add_card) FloatingActionButton mAddCard;
    @BindView(R.id.project_add_column) FloatingActionButton mAddColumn;

    private ColumnPagerAdapter mAdapter;
    private int mCurrentPosition = -1;
    private Loader mLoader;
    private Project mProject;
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
                    mAddCard.setVisibility(View.VISIBLE);
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
            final String text = ((EditText) dialog.findViewById(R.id.project_new_column)).getText().toString();
            if(!text.isEmpty()) {
                mRefresher.setRefreshing(true);
                mEditor.addColumn(new Editor.ColumnAdditionListener() {
                    @Override
                    public void columnAdded(Column column) {
                        mAddCard.setVisibility(View.VISIBLE);
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
                            if(mAdapter.columns.size() == 0) mAddCard.setVisibility(View.GONE);

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

    void moveColumn(int tag, int dropTag) {
        final int from = mAdapter.indexOf(tag);
        final int to = mAdapter.indexOf(dropTag);
        Log.i(TAG, "moveColumn: From " + from + ", to " + to);
        mAdapter.move(from, to);
        mAdapter.columns.add(to, mAdapter.columns.remove(from));
        mColumnPager.setCurrentItem(to, true);
    }

    @OnClick(R.id.project_add_card)
    void addCard() {
        mMenu.close(true);
        final CardDialog dialog = new CardDialog();
        showCardDialog(dialog);
    }

    void editCard(Card card) {
        final CardDialog dialog = new CardDialog();
        final Bundle b = new Bundle();
        b.putParcelable(getString(R.string.parcel_card), card);
        dialog.setArguments(b);
        showCardDialog(dialog);
    }

    void deleteCard(Card card) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_delete_card)
                .setMessage(R.string.text_delete_note_warning)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_ok, (dialogInterface, i) -> {
                    mRefresher.setRefreshing(true);
                    final int columnId = mAdapter.getCurrentFragment().mColumn.getId();
                    mEditor.deleteCard(new Editor.CardDeletionListener() {
                        @Override
                        public void cardDeleted(Card card) {
                            mRefresher.setRefreshing(false);
                            mAdapter.getCurrentFragment().removeCard(card);
                            Snackbar.make(findViewById(R.id.project_coordinator),
                                    getString(R.string.text_note_deleted), Snackbar.LENGTH_LONG)
                                    .setAction(getString(R.string.action_undo), view -> mEditor.createCard(mCardCreationListener, columnId, card))
                                    .show();
                        }

                        @Override
                        public void cardDeletionError() {

                        }
                    }, card);
                }).show();
    }

    private Editor.CardCreationListener mCardCreationListener = new Editor.CardCreationListener() {
        @Override
        public void cardCreated(int columnId, Card card) {
            mAdapter.getExistingFragment(mAdapter.indexOf(columnId)).addCard(card);
            mRefresher.setRefreshing(false);
            final Bundle bundle = new Bundle();
            bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
            mAnalytics.logEvent(Analytics.TAG_CARD_CREATION, bundle);
        }

        @Override
        public void cardCreationError() {
            mRefresher.setRefreshing(false);
            final Bundle bundle = new Bundle();
            bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
            mAnalytics.logEvent(Analytics.TAG_CARD_CREATION, bundle);
        }
    };

    private void showCardDialog(CardDialog dialog) {
        final int columnPosition = mCurrentPosition;
        dialog.setListener(new CardDialog.CardListener() {
            @Override
            public void cardEditDone(Card card, boolean isNewCard) {
                mRefresher.setRefreshing(true);
                if(isNewCard) {
                    mEditor.createCard(mCardCreationListener, mAdapter.getCurrentFragment().mColumn.getId(), card);
                } else {
                    mEditor.updateCard(new Editor.CardUpdateListener() {
                        @Override
                        public void cardUpdated(Card card) {
                            mAdapter.getExistingFragment(columnPosition).updateCard(card);
                            mRefresher.setRefreshing(false);
                        }

                        @Override
                        public void cardUpdateError() {
                            mRefresher.setRefreshing(false);
                        }
                    }, card.getId(), card.getNote());
                }
            }

            @Override
            public void cardEditCancelled() {

            }
        });
        dialog.show(getSupportFragmentManager(), "TAG");
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_settings) {
            startActivity(new Intent(ProjectActivity.this, SettingsActivity.class));
        } else if(item.getItemId() == R.id.menu_source) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL)));
        }

        return true;
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
            return ColumnFragment.getInstance(((ColumnPageDescriptor) pageDescriptor).mColumn, mNavListener, mCanEdit);
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
