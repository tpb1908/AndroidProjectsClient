package com.tpb.projects.user;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.tpb.projects.R;

import butterknife.ButterKnife;

/**
 * Created by theo on 26/12/16.
 */

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme_Dark);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);



    }
}
