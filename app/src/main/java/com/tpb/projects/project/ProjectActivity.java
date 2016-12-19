package com.tpb.projects.project;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
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
import com.tpb.projects.data.models.Project;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 19/12/16.
 */

public class ProjectActivity extends AppCompatActivity implements Loader.ProjectLoader {

    @BindView(R.id.project_name) TextView mName;
    @BindView(R.id.project_new_card_fab) FloatingActionButton mNewCardFab;
    @BindView(R.id.project_column_pager) ViewPager mColumnPager;

    private ColumnPager mAdapter;

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

        new Handler().postDelayed(() -> mNewCardFab.show(), 400);
    }

    @Override
    public void projectLoaded(Project project) {
        mName.setText(project.getName());
    }

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        mNewCardFab.hide();
        super.onBackPressed();
    }

    private class ColumnPager extends FragmentPagerAdapter {

        ColumnPager(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return ColumnFragment.getInstance();
        }

        @Override
        public int getCount() {
            return 3;
        }

    }
}
