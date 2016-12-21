package com.tpb.projects.project;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.commonsware.cwac.pager.PageDescriptor;
import com.commonsware.cwac.pager.v4.ArrayPagerAdapter;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.tpb.projects.R;
import com.tpb.projects.data.Editor;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.Column;
import com.tpb.projects.data.models.Project;
import com.tpb.projects.repo.ProjectAdapter;

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
    private static final String TAG = ProjectAdapter.class.getSimpleName();

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme_Dark);
        setContentView(R.layout.activity_project);
        ButterKnife.bind(this);

        final Intent launchIntent = getIntent();
        mLoader = new Loader(this);
        mEditor = new Editor(this);
        if(launchIntent.hasExtra(getString(R.string.parcel_project))) {
            projectLoaded(launchIntent.getParcelableExtra(getString(R.string.parcel_project)));
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
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mColumnPager.setOffscreenPageLimit(mAdapter.getCount());
        mRefresher.setRefreshing(true);
        mMenu.hideMenuButton(false); //Hide the button so that we can show it later
        mMenu.setClosedOnTouchOutside(true);

        mRefresher.setOnRefreshListener(() -> mLoader.loadProject(ProjectActivity.this, mProject.getId()));


        //TODO Only add the card fab when we have columns
        new Handler().postDelayed(() -> mMenu.showMenuButton(true), 400);
    }

    @Override
    public void projectLoaded(Project project) {
        Log.i(TAG, "projectLoaded: Owner url " + project.getOwnerUrl());
        mProject = project;
        mName.setText(project.getName());
        mLoader.loadColumns(new Loader.ColumnsLoader() {
            @Override
            public void columnsLoaded(Column[] columns) {
                mRefresher.setRefreshing(false);
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
                mColumnPager.setCurrentItem(mCurrentPosition);
                mColumnPager.postDelayed(() -> mColumnPager.setVisibility(View.VISIBLE), 300);
            }

            @Override
            public void loadError() {

            }
        }, project.getId());
    }

    @Override
    public void loadError() {

    }

    @OnClick(R.id.project_add_column)
    void addColumn() {
        final AlertDialog dialog  = new AlertDialog.Builder(this)
                .setView(R.layout.dialog_new_column)
                .setTitle(R.string.title_new_column)
                .setNegativeButton(R.string.action_cancel, null)
                .create();
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.action_ok), (dialogInterface, i) -> {
            mRefresher.setRefreshing(true);
            final String text = ((EditText) dialog.findViewById(R.id.project_new_column)).getText().toString();
            //TODO Check for string length
            mEditor.addColumn(new Editor.ColumnAddListener() {
                @Override
                public void columnAdded(Column column) {
                    mAdapter.columns.add(column);
                    mAdapter.add(new ColumnPageDescriptor(column));
                    mColumnPager.setCurrentItem(mAdapter.getCount());
                    mRefresher.setRefreshing(false);
                }

                @Override
                public void addError() {

                }
            }, mProject.getId(), text);
        });

        dialog.show();
    }

    @OnClick(R.id.project_add_card)
    void test() {

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
                        }

                        @Override
                        public void deletionError() {

                        }
                    }, column.getId());
                }).show();

    }

    void loadIssue(Loader.IssueLoader loader, int issueId) {
        mLoader.loadIssue(loader, mProject.getRepoFullName(), issueId);
    }

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        if(mMenu.isOpened()) {
            mMenu.close(true);
        } else {
            mMenu.hideMenuButton(true);
            super.onBackPressed();
        }
    }

    private class ColumnPagerAdapter extends ArrayPagerAdapter<ColumnFragment> {
        private ArrayList<Column> columns;

        ColumnPagerAdapter(FragmentManager manager, List<PageDescriptor> descriptors) {
            super(manager, descriptors);
        }

        @Override
        protected ColumnFragment createFragment(PageDescriptor pageDescriptor) {
            return ColumnFragment.getInstance(((ColumnPageDescriptor) pageDescriptor).mColumn);
        }


    }

    private class ColumnPageDescriptor implements PageDescriptor {
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

        public final Creator<ColumnPageDescriptor> CREATOR = new Creator<ColumnPageDescriptor>() {
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
