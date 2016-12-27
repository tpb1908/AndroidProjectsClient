package com.tpb.projects.user;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.tpb.projects.BuildConfig;
import com.tpb.projects.R;

import butterknife.ButterKnife;


/**
 * Created by theo on 26/12/16.
 */

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = SettingsActivity.class.getSimpleName();

    private Preferences preferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = SettingsActivity.Preferences.getPreferences(this);
        setTheme(preferences.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);
        ((TextView) findViewById(R.id.text_version_number)).setText(BuildConfig.VERSION_NAME);
        ((Switch) findViewById(R.id.switch_dark_theme)).setChecked(preferences.isDarkThemeEnabled());
        ((Switch) findViewById(R.id.switch_enable_analytics)).setChecked(preferences.areAnalyticsEnabled());
    }

    public void onSettingsClick(View view) {
        switch(view.getId()) {
            case R.id.switch_dark_theme:
                Log.i(TAG, "onSettingsClick: Toggle dark theme");
                preferences.setDarkThemeEnabled(((Switch) view).isChecked());
                break;
            case R.id.switch_enable_analytics:
                Log.i(TAG, "onSettingsClick: Toggle analytics");
                preferences.setAnalyticsEnabled(((Switch) view).isChecked());
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
            case R.id.layout_settings_repository:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/tpb1908/AndroidProjectsClient")));
                break;
            case R.id.layout_settings_developer:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/tpb1908")));
                break;
        }
    }

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }

    public static class Preferences {
        private static final String TAG = Preferences.class.getSimpleName();
        private static Preferences preferences;
        private static final String KEY = "PREFS";

        private static final String KEY_DARK_THEME = "DT";
        private static final String KEY_ANALYTICS = "A";
        private static final String KEY_CARD_ACTION = "C";

        private SharedPreferences prefs;

        private Preferences(Context context) {
            prefs = context.getSharedPreferences(KEY, Context.MODE_PRIVATE);
        }

        public static Preferences getPreferences(Context context) {
            if(preferences == null) preferences = new Preferences(context);
            return preferences;
        }

        public boolean isDarkThemeEnabled() {
            return prefs.getBoolean(KEY_DARK_THEME, true);
        }

        public boolean areAnalyticsEnabled() {
            return prefs.getBoolean(KEY_ANALYTICS, true);
        }

        public CardAction getCardAction() {
            switch(prefs.getInt(KEY_CARD_ACTION, 0)) {
                case 0:
                    return CardAction.EDIT;
                case 1:
                    return CardAction.FULLSCREEN;
                default:
                    return CardAction.COPY;
            }
        }

        public void setDarkThemeEnabled(boolean enabled) {
            prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply();
        }

        public void setAnalyticsEnabled(boolean enabled) {
            prefs.edit().putBoolean(KEY_ANALYTICS, enabled).apply();
        }

        public void setCardAction(CardAction action) {
            int a = 0;
            switch(action) {
                case EDIT:
                    a = 0;
                    break;
                case FULLSCREEN:
                    a = 1;
                    break;
                case COPY:
                    a = 2;
                    break;
            }
            prefs.edit().putInt(KEY_CARD_ACTION, a).apply();
        }

        public enum CardAction {
            EDIT, FULLSCREEN, COPY
        }

    }

}
