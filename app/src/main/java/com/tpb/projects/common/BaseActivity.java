package com.tpb.projects.common;

import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.tpb.projects.util.UI;

/**
 * Created by theo on 10/03/17.
 */

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UI.removeActivityFromTransitionManager(this);
    }

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }

}
