package com.tpb.projects.util;

import android.support.v7.app.AppCompatActivity;

/**
 * Created by theo on 10/03/17.
 */

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UI.removeActivityFromTransitionManager(this);
    }
}
