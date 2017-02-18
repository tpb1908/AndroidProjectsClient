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
import com.tpb.projects.issues.IssueActivity;
import com.tpb.projects.issues.IssuesActivity;
import com.tpb.projects.project.ProjectActivity;
import com.tpb.projects.repo.RepoActivity;
import com.tpb.projects.repo.content.ContentActivity;
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
                    final Intent u = new Intent(Interceptor.this, UserActivity.class);
                    u.putExtra(getString(R.string.intent_username), segments.get(0));
                    startActivity(u);
                    overridePendingTransition(R.anim.slide_up, R.anim.none);
                    finish();
                    break;
                case 2: //Repo
                    final Intent r = new Intent(Interceptor.this, RepoActivity.class);
                    r.putExtra(getString(R.string.intent_repo), segments.get(0) + "/" + segments.get(1));
                    startActivity(r);
                    overridePendingTransition(R.anim.slide_up, R.anim.none);
                    finish();
                    break;
                case 3:
                    if("projects".equals(segments.get(2))) {
                        final Intent pr = new Intent(Interceptor.this, RepoActivity.class);
                        pr.putExtra(getString(R.string.intent_repo), segments.get(0) + "/" + segments.get(1));
                        startActivity(pr);
                        overridePendingTransition(R.anim.slide_up, R.anim.none);
                        finish();
                    } else if("issues".equals(segments.get(2))) {
                        final Intent pr = new Intent(Interceptor.this, IssuesActivity.class);
                        pr.putExtra(getString(R.string.intent_repo), segments.get(0) + "/" + segments.get(1));
                        startActivity(pr);
                        overridePendingTransition(R.anim.slide_up, R.anim.none);
                        finish();
                    } else {
                        fail();
                    }
                    break;
                case 4: //Project
                    if("projects".equals(segments.get(2))) {
                        final Intent p = new Intent(Interceptor.this, ProjectActivity.class);
                        p.putExtra(getString(R.string.intent_repo), segments.get(0) + "/" + segments.get(1));
                        p.putExtra(getString(R.string.intent_project_number), Integer.parseInt(segments.get(3)));
                        final String path = getIntent().getDataString();

                        final StringBuilder id = new StringBuilder();
                        for(int i = path.indexOf('#', path.indexOf(segments.get(3))) + 6; i < path.length(); i++) {
                            if(path.charAt(i) >= '0' && path.charAt(i) <= '9') id.append(path.charAt(i));
                        }
                        try {
                            final int cardId = Integer.parseInt(id.toString());
                            p.putExtra(getString(R.string.intent_card_id), cardId);
                        } catch(Exception ignored) {}
                        startActivity(p);
                        overridePendingTransition(R.anim.slide_up, R.anim.none);
                        finish();
                    } else if("issues".equals(segments.get(2))) {
                        final Intent i = new Intent(Interceptor.this, IssueActivity.class);
                        i.putExtra(getString(R.string.intent_repo), segments.get(0) + "/" + segments.get(1));
                        i.putExtra(getString(R.string.intent_issue_number), Integer.parseInt(segments.get(3)));
                        startActivity(i);
                        overridePendingTransition(R.anim.slide_up, R.anim.none);
                        finish();
                    } else {
                        fail();
                    }
                    break;
                default:
                    if("tree".equals(segments.get(2))) {
                        final Intent content = new Intent(Interceptor.this, ContentActivity.class);
                        content.putExtra(getString(R.string.intent_repo), segments.get(0) + "/" + segments.get(1));
                        final StringBuilder path = new StringBuilder();
                        for(int i = 3; i < segments.size(); i++){
                            path.append(segments.get(i));
                            path.append('/');
                        }
                        Log.i(TAG, "onCreate: Path is " + path);
                        content.putExtra(getString(R.string.intent_path), path.toString());
                        startActivity(content);
                        overridePendingTransition(R.anim.slide_up, R.anim.none);
                        finish();
                    } else if("blob".equals(segments.get(2))) {
                        String path = "";
                        for(int i = 3; i < segments.size(); i++) path += segments.get(i) + "/";
                        Log.i(TAG, "onCreate: Blob path is " + path);
                        fail();
                    } else {
                        fail();
                    }
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
            final Intent intent = new Intent(getIntent().getAction());
            intent.setData(getIntent().getData());
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.addCategory(Intent.CATEGORY_DEFAULT);

            final List<ResolveInfo> resolvedInfo = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

            if (!resolvedInfo.isEmpty()) {
                final List<Intent> targetedShareIntents = new ArrayList<>();

                for (ResolveInfo resolveInfo : resolvedInfo) {
                    final String packageName = resolveInfo.activityInfo.packageName;
                    if (!packageName.equals(getPackageName())) {
                        final Intent targetedShareIntent = new Intent(getIntent().getAction());
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

