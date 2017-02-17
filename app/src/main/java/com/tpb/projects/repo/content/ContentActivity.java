package com.tpb.projects.repo.content;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.FileLoader;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.models.files.Node;

import java.util.List;

import butterknife.ButterKnife;

/**
 * Created by theo on 17/02/17.
 */

public class ContentActivity extends AppCompatActivity {
    private static final String TAG = ContentActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences.getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        setContentView(R.layout.activity_content);
        ButterKnife.bind(this);

        final Intent launchIntent = getIntent();
        final String repo = launchIntent.getStringExtra(getString(R.string.intent_repo));
        new FileLoader(this).loadDirectory(new FileLoader.DirectoryLoader() {
            @Override
            public void directoryLoaded(List<Node> directory) {
                Log.i(TAG, "directoryLoaded: " + directory);
            }

            @Override
            public void directoryLoadError(APIHandler.APIError error) {
                Log.i(TAG, "directoryLoadError: " + error);
            }
        }, repo, null);
    }
}
