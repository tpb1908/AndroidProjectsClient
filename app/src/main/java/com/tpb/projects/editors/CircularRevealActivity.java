package com.tpb.projects.editors;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import com.tpb.projects.R;
import com.tpb.projects.data.SettingsActivity;

/**
 * Created by theo on 20/02/17.
 */

public class CircularRevealActivity extends AppCompatActivity {

    private int x = -1;
    private int y = -1;
    private boolean mIsClosing = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(CircularRevealActivity.class.getSimpleName(), "onCreate: Reveal called");
        final Intent intent = getIntent();
        if(intent != null &&
                intent.hasExtra(getString(R.string.intent_position_x)) &&
                intent.hasExtra(getString(R.string.intent_position_y))) {
            Log.i(CircularRevealActivity.class.getSimpleName(), "onCreate: Has positions");
            x = intent.getIntExtra(getString(R.string.intent_position_x), -1);
            y = intent.getIntExtra(getString(R.string.intent_position_y), -1);
        }
        final View content = findViewById(android.R.id.content);
        if(x != -1 && y != -1) {
            if(content.getViewTreeObserver().isAlive()) {
                content.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if(SettingsActivity.Preferences.getPreferences(CircularRevealActivity.this).isDarkThemeEnabled()) {
                            content.setBackgroundColor(getResources().getColor(R.color.defaultBackgroundDark));
                        } else {
                            content.setBackgroundColor(getResources().getColor(R.color.defaultBackgroundLight));
                        }
                        circularRevealActivity(x, y);
                        content.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
            }
        } else {
            if(SettingsActivity.Preferences.getPreferences(CircularRevealActivity.this).isDarkThemeEnabled()) {
                content.setBackgroundColor(getResources().getColor(R.color.defaultBackgroundDark));
            } else {
                content.setBackgroundColor(getResources().getColor(R.color.defaultBackgroundLight));
            }
        }
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }


    private void circularRevealActivity(int cx, int cy) {
        final View rootLayout = findViewById(android.R.id.content);

        float finalRadius = Math.max(rootLayout.getWidth(), rootLayout.getHeight());

        // create the animator for this view (the start radius is zero)
        Animator circularReveal = ViewAnimationUtils.createCircularReveal(rootLayout, cx, cy, 0, finalRadius);
        circularReveal.setDuration(600);

        // make the view visible and start the animation
        rootLayout.setVisibility(View.VISIBLE);
        circularReveal.start();
    }

    private void circularCloseActivity(int cx, int cy) {
        final View rootLayout = findViewById(android.R.id.content);

        // We expand to the larger of the two bounds
        float finalRadius = Math.max(rootLayout.getWidth(), rootLayout.getHeight());

        // Create the animator for this view (the start radius is zero)
        final Animator circularClose = ViewAnimationUtils.createCircularReveal(rootLayout, cx, cy, finalRadius, 0);
        //Generally 4-500 ms depending on system settings
        circularClose.setDuration(getResources().getInteger(android.R.integer.config_longAnimTime));
        circularClose.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                findViewById(android.R.id.content).setVisibility(View.GONE);
                super.onAnimationEnd(animation);
                CircularRevealActivity.super.finish();
            }
        });
        // make the view visible and start the animation
        mIsClosing = true;
        circularClose.start();
    }

    boolean isClosing() {
        return mIsClosing;
    }

    @Override
    public void finish() {
        if(x != -1 && y != -1) {
            circularCloseActivity(x, y);
        } else {
            super.finish();
            overridePendingTransition(0, 0); //Stop any other animations
        }
    }
}
