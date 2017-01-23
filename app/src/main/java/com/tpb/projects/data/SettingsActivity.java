/*
 * Copyright  2016 Theo Pearson-Bray
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.tpb.projects.data;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.tpb.projects.BuildConfig;
import com.tpb.projects.R;


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
        ((TextView) findViewById(R.id.text_version_number)).setText(BuildConfig.VERSION_NAME);
        ((Switch) findViewById(R.id.switch_dark_theme)).setChecked(preferences.isDarkThemeEnabled());
        ((Switch) findViewById(R.id.switch_enable_analytics)).setChecked(preferences.areAnalyticsEnabled());

        final Spinner spinner = (Spinner) findViewById(R.id.spinner_card_click);

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                getResources().getStringArray(R.array.settings_card_actions));
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                preferences.setCardAction(Preferences.CardAction.fromInt(i));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        spinner.setSelection(preferences.getCardAction().toInt());

        Log.i(TAG, "onCreate: Emails " + BuildConfig.BUG_EMAIL + ", " + BuildConfig.FEATURE_REQUEST_EMAIL);
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
                new LicenseDialogBuilder(this)
                        .setTitle("Licenses")
                        .setPositiveButton(android.R.string.ok, null)
                        .create().show();
                break;
            case R.id.layout_settings_repository:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.REPO_ADDRESS)));
                break;
            case R.id.layout_settings_developer:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/tpb1908")));
                break;
            case R.id.layout_settings_bug_report:
                final Intent bugIntent = new Intent(Intent.ACTION_SEND);
                bugIntent.setType("text/email");
                bugIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {BuildConfig.BUG_EMAIL});
                bugIntent.putExtra(Intent.EXTRA_SUBJECT, "Android Projects bug report");
                bugIntent.putExtra(Intent.EXTRA_TEXT, "");
                startActivity(Intent.createChooser(bugIntent, "Send email:"));
                break;
            case R.id.layout_settings_feature_request:
                final Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setType("text/email");
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {BuildConfig.FEATURE_REQUEST_EMAIL});
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Android Projects feature request");
                emailIntent.putExtra(Intent.EXTRA_TEXT, "Bug:\n\n\nSteps to reproduce:");
                startActivity(Intent.createChooser(emailIntent, "Send email:"));
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
            return CardAction.fromInt(prefs.getInt(KEY_CARD_ACTION, 0));
        }

        public void setDarkThemeEnabled(boolean enabled) {
            prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply();
        }

        public void setAnalyticsEnabled(boolean enabled) {
            prefs.edit().putBoolean(KEY_ANALYTICS, enabled).apply();
        }

        public void setCardAction(CardAction action) {
            prefs.edit().putInt(KEY_CARD_ACTION, action.toInt()).apply();
        }

        public enum CardAction {
            EDIT, FULLSCREEN, COPY;

            int toInt() {
                switch(this) {
                    case EDIT:
                        return 0;
                    case FULLSCREEN:
                        return 1;
                    case COPY:
                        return 2;
                }
                return -1;
            }

            static CardAction fromInt(int i) {
                switch(i) {
                    case 0:
                        return EDIT;
                    case 1:
                        return FULLSCREEN;
                    case 2:
                        return COPY;
                    default:
                        return EDIT;
                }
            }
        }

    }

    private static class LicenseDialogBuilder extends AlertDialog.Builder {

        private Context mContext;

        LicenseDialogBuilder(Context context) {
            super(context);
            mContext = context;
            createWebView();
        }

        private void createWebView() {
            final WebView wv = new WebView(mContext);
            wv.loadUrl("file:///android_asset/licenses_html.html");
            wv.setWebViewClient(new WebViewClient() {

                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                        view.getContext().startActivity(
                                new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                        return true;
                    } else {
                        return false;
                    }
                }
            });
            setView(wv);
        }

    }


}
