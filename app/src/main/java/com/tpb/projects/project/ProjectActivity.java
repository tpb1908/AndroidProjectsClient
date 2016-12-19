package com.tpb.projects.project;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.tpb.projects.R;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.Column;
import com.tpb.projects.data.models.Project;

import java.util.ArrayList;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 19/12/16.
 */

public class ProjectActivity extends AppCompatActivity implements Loader.ProjectLoader {

    @BindView(R.id.project_name) TextView mName;
    //@BindView(R.id.project_new_card_fab) FloatingActionButton mNewCardFab;
    @BindView(R.id.project_column_pager) ViewPager mColumnPager;

    private ColumnPager mAdapter;
    private Project mProject;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme_Dark);
        setContentView(R.layout.activity_project);
        ButterKnife.bind(this);

        final Intent launchIntent = getIntent();
        if(launchIntent.hasExtra(getString(R.string.parcel_project))) {
            projectLoaded(launchIntent.getParcelableExtra(getString(R.string.parcel_project)));
        }
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        mAdapter = new ColumnPager(getSupportFragmentManager());
        mColumnPager.setAdapter(mAdapter);
        mColumnPager.setOffscreenPageLimit(mAdapter.getCount());

       // new Handler().postDelayed(() -> mNewCardFab.show(), 400);
    }

    @Override
    public void projectLoaded(Project project) {
        mProject = project;
        mName.setText(project.getName());
        new Loader(this).loadColumns(new Loader.ColumnsLoader() {
            @Override
            public void columnsLoaded(Column[] columns) {
                mAdapter.columns = new ArrayList<>(Arrays.asList(columns));
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void loadError() {

            }
        }, project.getId());
    }

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
     //   mNewCardFab.hide();
        super.onBackPressed();
    }

    private class ColumnPager extends FragmentPagerAdapter {
        private ArrayList<Column> columns = new ArrayList<>();

        ColumnPager(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return ColumnFragment.getInstance(columns.get(position));
        }

        @Override
        public int getCount() {
            return columns.size();
        }

    }
}
