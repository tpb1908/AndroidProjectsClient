package com.tpb.projects.common;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.tpb.github.data.auth.OAuthHandler;
import com.tpb.projects.BuildConfig;
import com.tpb.projects.login.LoginActivity;
import com.tpb.projects.util.UI;

import com.tpb.projects.notifications.receivers.NotificationEventReceiver;

/**
 * Created by theo on 10/03/17.
 */

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final OAuthHandler OAuthHandler = new OAuthHandler(
                this,
                BuildConfig.GITHUB_CLIENT_ID,
                BuildConfig.GITHUB_CLIENT_SECRET,
                BuildConfig.GITHUB_REDIRECT_URL
        );
        if(!OAuthHandler.hasAccessToken() && !(this instanceof LoginActivity)) {
            final Intent intent = new Intent(BaseActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Intent.EXTRA_INTENT, getIntent());
            startActivity(intent);
        } else {
            NotificationEventReceiver.setupAlarm(getApplicationContext());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UI.removeActivityFromTransitionManager(this);
    }

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }

}
