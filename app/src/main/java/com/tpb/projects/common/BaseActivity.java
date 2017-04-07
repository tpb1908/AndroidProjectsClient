package com.tpb.projects.common;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.tpb.github.data.auth.GitHubSession;
import com.tpb.projects.login.LoginActivity;
import com.tpb.projects.notifications.receivers.NotificationEventReceiver;
import com.tpb.projects.util.UI;

/**
 * Created by theo on 10/03/17.
 */

public abstract class BaseActivity extends AppCompatActivity {

    public boolean mHasAccess = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!(this instanceof LoginActivity)) {
            if(GitHubSession.getSession(this).hasAccessToken()) {
                mHasAccess = true;
                NotificationEventReceiver.setupAlarm(getApplicationContext());
            } else {
                mHasAccess = false;
                final Intent intent = new Intent(BaseActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                if(getIntent() != null && !getIntent().getAction().equals(Intent.ACTION_MAIN)) {
                    intent.putExtra(Intent.EXTRA_INTENT, getIntent());
                }
                startActivity(intent);
                finish();
            }
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
