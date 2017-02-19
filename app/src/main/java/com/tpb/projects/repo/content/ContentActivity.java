package com.tpb.projects.repo.content;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tpb.projects.R;
import com.tpb.projects.data.FileLoader;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.models.files.Node;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 17/02/17.
 */

public class ContentActivity extends AppCompatActivity {
    private static final String TAG = ContentActivity.class.getSimpleName();

    @BindView(R.id.content_title) TextView mTitle;
    @BindView(R.id.content_ribbon_scrollview) HorizontalScrollView mRibbonScrollView;
    @BindView(R.id.content_file_ribbon) LinearLayout mRibbon;
    @BindView(R.id.content_recycler) RecyclerView mRecycler;
    @BindView(R.id.content_refresher) SwipeRefreshLayout mRefresher;

    public static Node mLaunchNode;

    private ContentAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences.getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        setContentView(R.layout.activity_content);
        ButterKnife.bind(this);

        initRibbon();

        final Intent launchIntent = getIntent();
        final String repo = launchIntent.getStringExtra(getString(R.string.intent_repo));
        mTitle.setText(repo.substring(repo.indexOf('/') + 1));

        mAdapter = new ContentAdapter(new FileLoader(this), this, repo, null);
        mRecycler.setAdapter(mAdapter);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRefresher.setOnRefreshListener(() -> {
            mAdapter.reload();
        });
    }

    private void initRibbon() {
        final TextView view = (TextView) getLayoutInflater().inflate(R.layout.shard_ribbon_item, mRibbon, false);
        view.setText("Root");
        view.setOnClickListener((v) -> {
            mRibbon.removeAllViews();
            mRibbon.addView(view);
            mAdapter.moveToStart();
        });
        mRibbon.addView(view);
    }

    void flashRecycler() {

    }

    void addRibbonItem(final Node node) {
        final TextView view = (TextView) getLayoutInflater().inflate(R.layout.shard_ribbon_item, mRibbon, false);
        view.setText(node.getName());
        view.setFocusable(false);
        view.setOnClickListener(v -> {
            //FIXME- Dirty hack
            final ArrayList<View> views = new ArrayList<>();
            for(int i = 0; i <= mRibbon.indexOfChild(view); i++) {
                views.add(mRibbon.getChildAt(i));
            }
            mRibbon.removeAllViews();
            for(View item : views) {
                mRibbon.addView(item);
            }
//            final ViewGroup parent = (ViewGroup) view.getParent();
//            Log.i(TAG, "addRibbonItem: Focused child " + parent.getFocusedChild());
//            parent.requestChildFocus(view, parent.getFocusedChild());
//            parent.removeViews(parent.indexOfChild(view) + 1, parent.getChildCount());
            mAdapter.moveTo(node);
        });


        mRibbon.addView(view);
        mRibbon.postDelayed(() -> mRibbonScrollView.fullScroll(View.FOCUS_RIGHT), 17);

    }

    private void removeRibbonItems(Node node) {

    }

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        if(mRibbon.getChildCount() > 1) {
            //FIXME Dirty hack
            final ArrayList<View> views = new ArrayList<>();
            for(int i = 0; i < mRibbon.getChildCount() - 1; i++) {
                views.add(mRibbon.getChildAt(i));
            }
            mRibbon.removeAllViews();
            for(View v  : views) mRibbon.addView(v);
            mAdapter.moveBack();
        } else {
            finish();
        }

    }
}
