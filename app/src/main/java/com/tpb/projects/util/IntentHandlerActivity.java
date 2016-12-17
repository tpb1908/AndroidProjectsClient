package com.tpb.projects.util;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by theo on 17/12/16.
 */

public class IntentHandlerActivity extends AppCompatActivity {
    private static String TAG = IntentHandlerActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            final String path = getIntent().getDataString().substring(18);
            Log.i(TAG, "onCreate: " + path);
            switch(Data.countOccurrences(path, '/')) {
                case 1: //Username
                    Log.i(TAG, "onCreate: Username" + path);
                    break;
                case 2: //Repo
                    Log.i(TAG, "onCreate: Repository" + path);
                    break;
                case 3: //Possible project
                    Log.i(TAG, "onCreate: Project " + path);
                    break;
                default:
                    Toast.makeText(this, "Can't handle link", Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }
}
