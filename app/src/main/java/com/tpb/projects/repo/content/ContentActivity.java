package com.tpb.projects.repo.content;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.tpb.animatingrecyclerview.AnimatingRecyclerView;
import com.tpb.github.data.APIHandler;
import com.tpb.github.data.FileLoader;
import com.tpb.github.data.Loader;
import com.tpb.github.data.models.content.Node;
import com.tpb.projects.R;
import com.tpb.projects.common.BaseActivity;
import com.tpb.projects.util.SettingsActivity;
import com.tpb.projects.util.UI;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 17/02/17.
 */

public class ContentActivity extends BaseActivity implements Loader.ListLoader<Pair<String, String>> {
    private static final String TAG = ContentActivity.class.getSimpleName();

    @BindView(R.id.content_title) TextView mTitle;
    @BindView(R.id.content_ribbon_scrollview) HorizontalScrollView mRibbonScrollView;
    @BindView(R.id.content_file_ribbon) LinearLayout mRibbon;
    @BindView(R.id.content_recycler) AnimatingRecyclerView mRecycler;
    @BindView(R.id.content_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.content_branch_spinner) Spinner mBranchSpinner;

    public static Node mLaunchNode;

    private ContentAdapter mAdapter;
    private List<Pair<String, String>> mBranches;
    private String mDefaultRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences
                .getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        UI.setStatusBarColor(getWindow(), getResources().getColor(R.color.colorPrimaryDark));
        setContentView(R.layout.activity_content);
        ButterKnife.bind(this);

        initRibbon();

        final Intent launchIntent = getIntent();
        final String repo = launchIntent.getStringExtra(getString(R.string.intent_repo));
        mTitle.setText(repo.substring(repo.indexOf('/') + 1));

        mAdapter = new ContentAdapter(new FileLoader(this), this, repo, null);
        mRecycler.enableLineDecoration();
        mRecycler.setAdapter(mAdapter);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRefresher.setOnRefreshListener(() -> mAdapter.reload());
        new Loader(this).loadBranches(this, repo);
    }

    @Override
    public void listLoadComplete(List<Pair<String, String>> branches) {
        mBranches = branches;
        if(mDefaultRef != null) bindBranches();
    }

    public void setDefaultRef(String ref) {
        if(mDefaultRef == null) {
            mDefaultRef = ref;
            if(mBranches != null && !mBranches.isEmpty()) {
                bindBranches();
            }
        }
    }

    public void bindBranches() {
        final List<String> branchNames = new ArrayList<>(mBranches.size());
        for(Pair<String, String> p : mBranches) {
            if(mDefaultRef.equals(p.first)) {
                branchNames.add(0, p.first);
            } else {
                branchNames.add(p.first);
            }
        }
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, branchNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBranchSpinner.setAdapter(adapter);
        if(mBranchSpinner.getOnItemSelectedListener() == null) {
            mBranchSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    mAdapter.setRef(branchNames.get(position));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }
    }

    @Override
    public void listLoadError(APIHandler.APIError error) {

    }

    private void initRibbon() {
        final TextView view = (TextView) getLayoutInflater()
                .inflate(R.layout.shard_ribbon_item, mRibbon, false);
        view.setText(R.string.text_ribbon_root);
        view.setOnClickListener((v) -> {
            mRibbon.removeAllViews();
            mRibbon.addView(view);
            mAdapter.moveToStart();
        });
        mRibbon.addView(view);
    }

    void addRibbonItem(final Node node) {
        final TextView view = (TextView) getLayoutInflater()
                .inflate(R.layout.shard_ribbon_item, mRibbon, false);
        view.setText(node.getName());
        view.setFocusable(false);
        view.setOnClickListener(v -> {
            final ArrayList<View> views = new ArrayList<>();
            for(int i = 0; i <= mRibbon.indexOfChild(view); i++) {
                views.add(mRibbon.getChildAt(i));
            }

            mRibbon.removeAllViews();
            for(View item : views) mRibbon.addView(item);
            mAdapter.moveTo(node);
        });


        mRibbon.addView(view);
        mRibbon.post(() -> mRibbonScrollView.fullScroll(View.FOCUS_RIGHT));

    }


    @Override
    public void onBackPressed() {
        if(mRibbon.getChildCount() > 1) {
            final ArrayList<View> views = new ArrayList<>();
            for(int i = 0; i < mRibbon.getChildCount() - 1; i++) {
                views.add(mRibbon.getChildAt(i));
            }
            mRibbon.removeAllViews();
            for(View v : views) mRibbon.addView(v);
            mAdapter.moveBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onToolbarBackPressed(View view) {
        super.onBackPressed();
    }
}
