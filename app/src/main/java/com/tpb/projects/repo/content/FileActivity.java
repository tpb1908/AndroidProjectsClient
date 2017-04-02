package com.tpb.projects.repo.content;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
import com.pddstudio.highlightjs.HighlightJsView;
import com.pddstudio.highlightjs.models.Language;
import com.pddstudio.highlightjs.models.Theme;
import com.tpb.github.data.FileLoader;
import com.tpb.github.data.models.content.Node;
import com.tpb.projects.R;
import com.tpb.projects.util.SettingsActivity;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 19/02/17.
 */

public class FileActivity extends AppCompatActivity {
    private static final String TAG = FileActivity.class.getSimpleName();

    @BindView(R.id.file_name) TextView mName;
    @BindView(R.id.file_webview) HighlightJsView mWebView;
    @BindView(R.id.file_loading_spinner) ProgressBar mSpinner;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences
                .getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        setContentView(R.layout.activity_file);
        ButterKnife.bind(this);

        if(prefs.isDarkThemeEnabled()) {
            mWebView.setTheme(Theme.ANDROID_STUDIO);
        }
        mWebView.setZoomSupportEnabled(true);
        mWebView.setShowLineNumbers(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.setOnContentChangedListener(() -> {
            mSpinner.setVisibility(View.GONE);
            mWebView.setVisibility(View.VISIBLE);
        });
        final StringRequestListener fileLoadListener = new StringRequestListener() {
            @Override
            public void onResponse(String response) {
                mWebView.setSource(response.replace("&#", "&#38;&#35;"));
            }

            @Override
            public void onError(ANError anError) {
                Log.i(TAG, "onError: " + anError.toString());
                mSpinner.setVisibility(View.GONE);
            }
        };
        if(getIntent().hasExtra(getString(R.string.intent_blob_path))) {
            final String repo = getIntent().getStringExtra(getString(R.string.intent_repo));
            final String blob = getIntent().getStringExtra(getString(R.string.intent_blob_path));
            Log.i(TAG, "onCreate: Repo path is " + repo);
            Log.i(TAG, "onCreate: Blob path is " + blob);
            final int nameStart = blob.lastIndexOf('/') + 1;
            if(nameStart < blob.length()) {
                mName.setText(blob.substring(nameStart));
            }
            mWebView.setHighlightLanguage(getLanguage(getFileType(blob)));
            new FileLoader(this).loadRawFile(fileLoadListener,
                    "https://raw.githubusercontent.com/" + repo + blob
            );
        } else if(getIntent().hasExtra(getString(R.string.intent_gist_url))) {
            final String url = getIntent().getStringExtra(getString(R.string.intent_gist_url));
            mWebView.setHighlightLanguage(getLanguage(getFileType(url)));
            new FileLoader(this).loadRawFile(fileLoadListener, url);
        } else if(ContentActivity.mLaunchNode != null) {
            final Node node = ContentActivity.mLaunchNode;
            mName.setText(node.getName());
            mWebView.setHighlightLanguage(getLanguage(getFileType(node.getUrl())));
            new FileLoader(this).loadRawFile(fileLoadListener, node.getDownloadUrl());
        } else {
            finish();
        }
    }

    private static String getFileType(String path) {
        final int qIndex = path.lastIndexOf('?');
        return path.substring(path.lastIndexOf('.') + 1, qIndex > 0 ? qIndex : path.length());
    }

    private static Language getLanguage(String lang) {
        for(Language l : Language.values()) {
            if(l.toString().equalsIgnoreCase(lang)) return l;
        }
        return Language.AUTO_DETECT;
    }

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }

}
