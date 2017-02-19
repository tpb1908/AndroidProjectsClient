package com.tpb.projects.repo.content;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
import com.pddstudio.highlightjs.HighlightJsView;
import com.pddstudio.highlightjs.models.Theme;
import com.tpb.projects.R;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.models.files.Node;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 19/02/17.
 */

public class FileActivity extends AppCompatActivity {
    private static final String TAG = FileActivity.class.getSimpleName();

    @BindView(R.id.file_name) TextView mName;
    @BindView(R.id.file_webview) HighlightJsView mWebView;

    private Node mNode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences.getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        setContentView(R.layout.activity_file);
        ButterKnife.bind(this);

        if(prefs.isDarkThemeEnabled()) {
            mWebView.setTheme(Theme.ANDROID_STUDIO);
        }

        mNode = ContentActivity.mLaunchNode;
        mName.setText(mNode.getName());
        Log.i(TAG, "onCreate: Download URL " + mNode.getDownloadUrl());
        AndroidNetworking.get(mNode.getDownloadUrl())
                .setPriority(Priority.IMMEDIATE)
                .build()
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String response) {
                        Log.i(TAG, "onResponse: " + response);
                        mWebView.setSource(response);
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: " + anError.toString());
                    }
                });
    }

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }

}
