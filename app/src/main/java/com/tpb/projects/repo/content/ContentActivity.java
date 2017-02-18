package com.tpb.projects.repo.content;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.FileLoader;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.models.files.Node;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 17/02/17.
 */

public class ContentActivity extends AppCompatActivity {
    private static final String TAG = ContentActivity.class.getSimpleName();

    @BindView(R.id.content_file_ribbon) LinearLayout mRibbon;
    @BindView(R.id.content_recycler) RecyclerView mRecycler;
    @BindView(R.id.content_refresher) SwipeRefreshLayout mRefresher;

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

        mAdapter = new ContentAdapter();
        mRecycler.setAdapter(mAdapter);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));

        new FileLoader(this).loadDirectory(new FileLoader.DirectoryLoader() {
            @Override
            public void directoryLoaded(List<Node> directory) {
                Log.i(TAG, "directoryLoaded: " + directory);
                mAdapter.setNodes(directory);
            }

            @Override
            public void directoryLoadError(APIHandler.APIError error) {
                Log.i(TAG, "directoryLoadError: " + error);
            }
        }, repo, null);
    }

    private void initRibbon() {
        final TextView view = (TextView) getLayoutInflater().inflate(R.layout.shard_ribbon_item, mRibbon, false);
        view.setText("Root");
        mRibbon.addView(view);
    }

    private void addRibbonItem(Node node) {
        final TextView view = (TextView) getLayoutInflater().inflate(R.layout.shard_ribbon_item, mRibbon, false);
        view.setText(node.getName());
        mRibbon.addView(view);
    }

    private void removeRibbonItems(Node node) {

    }

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }
}
