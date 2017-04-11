package com.tpb.projects.common;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.transition.Transition;
import android.support.transition.TransitionManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.ArrayMap;
import android.view.View;
import android.view.ViewGroup;

import com.androidnetworking.AndroidNetworking;
import com.tpb.github.data.auth.GitHubSession;
import com.tpb.projects.login.LoginActivity;
import com.tpb.projects.notifications.receivers.NotificationEventReceiver;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by theo on 10/03/17.
 */

public abstract class BaseActivity extends AppCompatActivity {

    public boolean mHasAccess = true;
    private final List<WeakReference<Fragment>> mWeakFragments = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeActivityFromTransitionManager();
        cancelNetworkRequests();
    }

    private void removeActivityFromTransitionManager() {
        final Class transitionManagerClass = TransitionManager.class;
        try {
            final Field runningTransitionsField = transitionManagerClass
                    .getDeclaredField("sRunningTransitions");
            runningTransitionsField.setAccessible(true);
            //noinspection unchecked
            final ThreadLocal<WeakReference<ArrayMap<ViewGroup, ArrayList<Transition>>>> runningTransitions
                    = (ThreadLocal<WeakReference<ArrayMap<ViewGroup, ArrayList<Transition>>>>)
                    runningTransitionsField.get(transitionManagerClass);
            if(runningTransitions.get() == null || runningTransitions.get().get() == null) {
                return;
            }
            ArrayMap map = runningTransitions.get().get();
            View decorView = getWindow().getDecorView();
            if(map.containsKey(decorView)) {
                map.remove(decorView);
            }
        } catch(Exception ignored) {
        }
    }

    @Override
    public void onAttachFragment (Fragment fragment) {
        mWeakFragments.add(new WeakReference<>(fragment));
    }

    private void cancelNetworkRequests() {
        AndroidNetworking.cancel(this);
        for(WeakReference<Fragment> ref : mWeakFragments) {
            if(ref.get() != null) {
                AndroidNetworking.cancel(ref.get());
            }
        }
    }

}
