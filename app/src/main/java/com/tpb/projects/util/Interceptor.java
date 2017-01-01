/*
   * Credit to Gitskarios
   * https://github.com/gitskarios/Gitskarios/blob/develop/app/src/main/java/com/alorma/github/Interceptor.java
   * https://github.com/gitskarios/Gitskarios
 */

package com.tpb.projects.util;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import com.tpb.projects.R;
import com.tpb.projects.user.UserActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by theo on 01/01/17.
 */

public class Interceptor extends Activity {
    private static final String TAG = Interceptor.class.getSimpleName();

    private Intent failIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createFailIntent();

        if(getIntent().getAction().equals(Intent.ACTION_VIEW) &&
                getIntent().getData() != null &&
                "github.com".equals(getIntent().getData().getHost())) {
            final List<String> segments = getIntent().getData().getPathSegments();
            Log.i(TAG, "onCreate: Path: " + segments.toString());
            switch(segments.size()) {
                case 1: //User
                    final Intent i = new Intent(Interceptor.this, UserActivity.class);
                    i.putExtra(getString(R.string.intent_username), segments.get(0));
                    startActivity(i);
                    finish();
                    break;
                default:
                    fail();
            }
        } else {
            fail();
        }
    }

    private void fail() {
        Log.i(TAG, "fail: ");
        try {
            if (failIntent != null) {
                startActivity(failIntent);
                finish();
            } else {
                startActivity(onFail());
                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }

    private void createFailIntent() {
        new AsyncTask<Void, Void, Intent>() {

            @Override
            protected Intent doInBackground(Void... params) {
                return onFail();
            }

            @Override
            protected void onPostExecute(Intent intent) {
                super.onPostExecute(intent);
                Interceptor.this.failIntent = intent;
            }
        }.execute();
    }

    private Intent onFail() {
        Log.i(TAG, "onFail: ");
        if (failIntent == null && getIntent() != null) {
            return generateFailIntentWithoutApp();
        } else {
            return failIntent;
        }
    }

    private Intent generateFailIntentWithoutApp() {
        try {
            Intent intent = new Intent(getIntent().getAction());
            intent.setData(getIntent().getData());
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.addCategory(Intent.CATEGORY_DEFAULT);

            final List<ResolveInfo> resolveInfos = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

            if (!resolveInfos.isEmpty()) {
                final List<Intent> targetedShareIntents = new ArrayList<>();

                for (ResolveInfo resolveInfo : resolveInfos) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    if (!packageName.equals(getPackageName())) {
                        Intent targetedShareIntent = new Intent(getIntent().getAction());
                        targetedShareIntent.setData(getIntent().getData());
                        targetedShareIntent.setPackage(packageName);
                        targetedShareIntents.add(targetedShareIntent);
                    }
                }
                final Intent chooserIntent = Intent.createChooser(targetedShareIntents.remove(0), "Open with...");
                if (targetedShareIntents.size() > 0) {
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedShareIntents.toArray(new Parcelable[targetedShareIntents.size()]));
                }

                return chooserIntent;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return failIntent;
    }

}

