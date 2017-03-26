package com.tpb.projects.flow;

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
import com.tpb.projects.milestones.MilestoneActivity;
import com.tpb.projects.milestones.MilestonesActivity;
import com.tpb.projects.project.ProjectActivity;
import com.tpb.projects.repo.RepoActivity;
import com.tpb.projects.repo.content.ContentActivity;
import com.tpb.projects.repo.content.FileActivity;
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
            if(segments.size() == 1) {
                final Intent u = new Intent(Interceptor.this, UserActivity.class);
                u.putExtra(getString(R.string.intent_username), segments.get(0));
                startActivity(u);
                overridePendingTransition(R.anim.slide_up, R.anim.none);
                finish();
            } else {
                final Intent i = new Intent();
                putRepo(i, segments);
                switch(segments.size()) {
                    case 2: //Repo
                        i.setClass(Interceptor.this, RepoActivity.class);
                        startActivity(i);
                        overridePendingTransition(R.anim.slide_up, R.anim.none);
                        finish();
                        break;
                    case 3:
                        if("projects".equals(segments.get(2))) {
                            i.setClass(Interceptor.this, RepoActivity.class);
                            startActivity(i);
                            overridePendingTransition(R.anim.slide_up, R.anim.none);
                            finish();
                        } else if("issues".equals(segments.get(2))) {
                            i.setClass(Interceptor.this, RepoActivity.class);
                            i.putExtra(getString(R.string.intent_pager_page), RepoActivity.PAGE_ISSUES);
                            startActivity(i);
                            overridePendingTransition(R.anim.slide_up, R.anim.none);
                            finish();
                        } else if("milestones".equals(segments.get(2))) {
                            i.setClass(Interceptor.this, MilestonesActivity.class);
                            startActivity(i);
                            overridePendingTransition(R.anim.slide_up, R.anim.none);
                            finish();
                        } else {
                            fail();
                        }
                        break;
                    case 4: //Project
                        if("projects".equals(segments.get(2))) {
                            i.setClass(Interceptor.this, ProjectActivity.class);
                            i.putExtra(getString(R.string.intent_project_number), safelyExtractInt(segments.get(3)));
                            final String path = getIntent().getDataString();
                            final StringBuilder id = new StringBuilder();
                            for(int j = path.indexOf('#', path.indexOf(segments.get(3))) + 6; j < path.length(); j++) {
                                if(path.charAt(j) >= '0' && path.charAt(j) <= '9') id.append(path.charAt(j));
                            }
                            try {
                                i.putExtra(getString(R.string.intent_card_id), safelyExtractInt(id.toString()));
                            } catch(Exception ignored) {}

                            startActivity(i);
                            overridePendingTransition(R.anim.slide_up, R.anim.none);
                            finish();
                        } else if("issues".equals(segments.get(2))) {
                            i.setClass(Interceptor.this, IssueActivity.class);
                            i.putExtra(getString(R.string.intent_issue_number), safelyExtractInt(segments.get(3)));
                            startActivity(i);
                            overridePendingTransition(R.anim.slide_up, R.anim.none);
                            finish();
                        } else if("milestone".equals(segments.get(2))) {
                            i.setClass(Interceptor.this, MilestonesActivity.class);
                            i.putExtra(getString(R.string.intent_milestone_number), safelyExtractInt(segments.get(3)));
                            startActivity(i);
                            finish();
                        } else {
                            fail();
                        }
                        break;
                    default:
                        if("tree".equals(segments.get(2))) {
                            i.setClass(Interceptor.this, ContentActivity.class);
                            i.putExtra(getString(R.string.intent_repo), segments.get(0) + "/" + segments.get(1));
                            final StringBuilder path = new StringBuilder();
                            for(int j = 3; j < segments.size(); j++) {
                                path.append(segments.get(j));
                                path.append('/');
                            }
                            i.putExtra(getString(R.string.intent_path), path.toString());
                            startActivity(i);
                            overridePendingTransition(R.anim.slide_up, R.anim.none);
                            finish();
                        } else if("blob".equals(segments.get(2))) {
                            i.setClass(Interceptor.this, FileActivity.class);
                            final StringBuilder path = new StringBuilder();
                            for(int j = 2; j < segments.size(); j++) {
                                path.append('/');
                                path.append(segments.get(j));
                            }
                            i.putExtra(getString(R.string.intent_blob_path), path.toString());
                            startActivity(i);
                            overridePendingTransition(R.anim.slide_up, R.anim.none);
                            finish();
                        } else if("milestone".equals(segments.get(2))) {
                            //TODO Deal with number and edit suffix
                            i.setClass(Interceptor.this, MilestoneActivity.class);
                            i.putExtra(getString(R.string.intent_milestone_number), safelyExtractInt(segments.get(3)));
                            startActivity(i);
                            finish();
                        } else {
                            fail();
                        }
                }
            }

        } else {
            fail();
        }
    }

    private static int safelyExtractInt(String possibleInt) {
        try {
            return Integer.parseInt(possibleInt.replace("\\s+", ""));
        } catch(NumberFormatException nfe) {
            return -1;
        }
    }

    private void putRepo(Intent i, List<String> segments) {
        i.putExtra(getString(R.string.intent_repo), segments.get(0) + "/" + segments.get(1));
    }

    private void fail() {
        Log.i(TAG, "fail: ");
        try {
            if(failIntent != null) {
                startActivity(failIntent);
                finish();
            } else {
                startActivity(onFail());
                finish();
            }
        } catch(Exception e) {
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
        if(failIntent == null && getIntent() != null) {
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

            if(!resolvedInfo.isEmpty()) {
                final List<Intent> targetedShareIntents = new ArrayList<>();
                for(ResolveInfo resolveInfo : resolvedInfo) {
                    final String packageName = resolveInfo.activityInfo.packageName;
                    if(!packageName.equals(getPackageName())) {
                        final Intent targetedShareIntent = new Intent(getIntent().getAction());
                        targetedShareIntent.setData(getIntent().getData());
                        targetedShareIntent.setPackage(packageName);
                        targetedShareIntents.add(targetedShareIntent);
                    }
                }
                final Intent chooserIntent = Intent.createChooser(targetedShareIntents.remove(0), getString(R.string.text_interceptor_open_with));
                if(targetedShareIntents.size() > 0) {
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedShareIntents.toArray(new Parcelable[targetedShareIntents.size()]));
                }
                return chooserIntent;
            }
        } catch(Exception ignored) {
        }
        return failIntent;
    }

}

