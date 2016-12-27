package com.tpb.projects.user;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.tpb.projects.BuildConfig;
import com.tpb.projects.R;

import butterknife.ButterKnife;

/**
 * Created by theo on 26/12/16.
 */

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = SettingsActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme_Dark);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);
        ((TextView) findViewById(R.id.text_version_number)).setText(BuildConfig.VERSION_NAME);
    }

    public void onSettingsClick(View view) {
        switch(view.getId()) {
            case R.id.switch_dark_theme:
                Log.i(TAG, "onSettingsClick: Toggle dark theme");
                break;
            case R.id.switch_enable_analytics:
                Log.i(TAG, "onSettingsClick: Toggle analytics");
                break;
            case R.id.layout_settings_version:
                Log.i(TAG, "onSettingsClick: Display version");
                break;
            case R.id.layout_settings_changelog:
                Log.i(TAG, "onSettingsClick: Display changelog");
                break;
            case R.id.layout_settings_licenses:
                Log.i(TAG, "onSettingsClick: Display licenses");
                break;
        }
    }

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }
}
