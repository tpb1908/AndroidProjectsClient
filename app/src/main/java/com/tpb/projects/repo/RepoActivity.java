/*
 * Copyright  2016 Theo Pearson-Bray
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.tpb.projects.repo;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.widget.ANImageView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.mittsu.markedview.MarkedView;
import com.tpb.animatingrecyclerview.AnimatingRecycler;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Editor;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.auth.GitHubSession;
import com.tpb.projects.data.models.Project;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.editors.ProjectDialog;
import com.tpb.projects.issues.IssuesActivity;
import com.tpb.projects.project.ProjectActivity;
import com.tpb.projects.repo.content.ContentActivity;
import com.tpb.projects.user.UserActivity;
import com.tpb.projects.util.Analytics;
import com.tpb.projects.util.Constants;
import com.tpb.projects.util.Data;
import com.tpb.projects.util.ShortcutDialog;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.view.View.GONE;

/**
 * Created by theo on 16/12/16.
 */

public class RepoActivity extends AppCompatActivity implements
        Loader.RepositoryLoader,
        Loader.ReadMeLoader,
        Loader.ProjectsLoader,
        ProjectAdapter.ProjectEditor,
        ProjectDialog.ProjectListener,
        Editor.ProjectCreationListener {
    private static final String TAG = RepoActivity.class.getSimpleName();
    private static final String URL = "https://github.com/tpb1908/AndroidProjectsClient/blob/master/app/src/main/java/com/tpb/projects/repo/RepoActivity.java";

    private FirebaseAnalytics mAnalytics;

    @BindView(R.id.repo_toolbar) Toolbar mToolbar;
    @BindView(R.id.repo_name) TextView mName;
    @BindView(R.id.repo_description) TextView mDescription;
    @BindView(R.id.user_image) ANImageView mUserImage;
    @BindView(R.id.user_name) TextView mUserName;

    @BindView(R.id.repo_forks) TextView mForks;
    @BindView(R.id.repo_issues) TextView mIssues;
    @BindView(R.id.repo_size) TextView mSize;
    @BindView(R.id.repo_stars) TextView mStars;
    @BindView(R.id.repo_watchers) TextView mWatchers;

    @BindView(R.id.repo_show_readme) Button mReadmeButton;
    @BindView(R.id.repo_readme) MarkedView mReadme;

    @BindView(R.id.repo_coordinator) CoordinatorLayout mCoordinator;
    @BindView(R.id.repo_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.repo_project_recycler) AnimatingRecycler mRecycler;

    @BindView(R.id.repo_new_project) Button mNewProjectButton;

    private ProjectAdapter mAdapter;
    private Loader mLoader;
    private Editor mEditor;
    private Repository mRepo;
    private Repository.AccessLevel mAccessLevel;
    private boolean mHasStarredRepo = false;
    private boolean mIsWatchingRepo = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences.getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        setContentView(R.layout.activity_repo);
        ButterKnife.bind(this);

        mAnalytics = FirebaseAnalytics.getInstance(this);
        mAnalytics.setAnalyticsCollectionEnabled(prefs.areAnalyticsEnabled());

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        final Intent launchIntent = getIntent();
        mReadmeButton.setOnClickListener((v) -> {
            if(mReadme.getVisibility() == GONE) {
                mReadme.setVisibility(View.VISIBLE);
                mReadmeButton.setText(R.string.text_hide_readme);
            } else {
                mReadme.setVisibility(GONE);
                mReadmeButton.setText(R.string.text_show_readme);
            }
        });
        mNewProjectButton.setOnClickListener((v) -> {
            final ProjectDialog dialog = new ProjectDialog();
            dialog.setListener(this);
            dialog.show(getSupportFragmentManager(), TAG);
        });
        mLoader = new Loader(this);
        mEditor = new Editor(this);
        mRefresher.setOnRefreshListener(() -> {
            if(mRepo != null) {
                mAdapter.clearProjects();
                mName.setText(null);
                mDescription.setText(null);
                mUserName.setText(null);
                mUserImage.setImageDrawable(null);
                mLoader.loadRepository(this, mRepo.getFullName());
            }
        });
        mAdapter = new ProjectAdapter(this, mRecycler);
        mRecycler.setAdapter(mAdapter);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        if(launchIntent.getParcelableExtra(getString(R.string.intent_repo)) != null) {
            repoLoaded(launchIntent.getParcelableExtra(getString(R.string.intent_repo)));
        } else {
            mLoader.loadRepository(this, launchIntent.getStringExtra(getString(R.string.intent_repo)));
        }
    }

    @OnClick({R.id.user_name, R.id.user_image})
    void openUser() {
        if(mRepo != null) {
            final Intent i = new Intent(RepoActivity.this, UserActivity.class);
            i.putExtra(getString(R.string.intent_username), mRepo.getUserLogin());
            if(mUserImage.getDrawable() != null) {
                Log.i(TAG, "openUser: Putting bitmap");
                i.putExtra(getString(R.string.intent_drawable), ((BitmapDrawable) mUserImage.getDrawable()).getBitmap());
            }
            startActivity(i,
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this,
                        new Pair<>(mUserName, getString(R.string.transition_username)),
                        new Pair<>(mUserImage, getString(R.string.transition_user_image))
                    ).toBundle()
            );
        }
    }

    @OnClick({R.id.repo_issues, R.id.repo_issues_text, R.id.repo_issues_drawable})
    void openIssues() {
        if(mRepo != null) {
            final Intent i = new Intent(RepoActivity.this, IssuesActivity.class);
            i.putExtra(getString(R.string.intent_repo), mRepo.getFullName());
            startActivity(i,
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                            this,
                            findViewById(R.id.repo_issues_text),
                            getString(R.string.transition_title)
                    ).toBundle()
            );
        }
    }

    @OnClick({R.id.repo_stars, R.id.repo_stars_text, R.id.repo_stars_drawable})
    void toggleStar() {
        final Editor.StarChangeListener listener = isStarred -> {
            mHasStarredRepo = isStarred;
            if(isStarred) {
                ((TextView) findViewById(R.id.repo_stars_text)).setText(R.string.text_unstar);
                mStars.setText(Integer.toString(Integer.parseInt(mStars.getText().toString()) + 1));
            } else {
                ((TextView) findViewById(R.id.repo_stars_text)).setText(R.string.text_star);
                mStars.setText(Integer.toString(Integer.parseInt(mStars.getText().toString()) - 1));
            }
        };

        if(mHasStarredRepo) {
            mEditor.unstarRepo(listener, mRepo.getFullName());
        } else {
            mEditor.starRepo(listener, mRepo.getFullName());
        }
    }

    @OnClick({R.id.repo_watchers, R.id.repo_watchers_text, R.id.repo_watchers_drawable})
    void toggleWatch() {
        final Editor.WatchChangeListener listener = isWatched -> {
            mIsWatchingRepo = isWatched;
            if(isWatched) {
                ((TextView) findViewById(R.id.repo_watchers_text)).setText(R.string.text_unwatch);
                mWatchers.setText(Integer.toString(Integer.parseInt(mWatchers.getText().toString()) + 1));
            } else {
                ((TextView) findViewById(R.id.repo_stars_text)).setText(R.string.text_watch);
                mWatchers.setText(Integer.toString(Integer.parseInt(mWatchers.getText().toString()) - 1));
            }
        };
        if(mIsWatchingRepo) {
            mEditor.unwatchRepo(listener, mRepo.getFullName());
        } else {
            mEditor.watchRepo(listener, mRepo.getFullName());
        }
    }

    @OnClick(R.id.repo_show_files)
    void showFiles() {
        final Intent i = new Intent(this, ContentActivity.class);
        i.putExtra(getString(R.string.intent_repo), mRepo.getFullName());
        startActivity(i);
    }

    @Override
    public void repoLoaded(Repository repo) {
        mRepo = repo;
        mName.setText(repo.getName());
        if(Constants.JSON_NULL.equals(repo.getDescription())) {
            mDescription.setVisibility(GONE);
        } else {
            mDescription.setText(repo.getDescription());
        }
        mUserName.setText(repo.getUserLogin());
        mUserImage.setImageUrl(repo.getUserAvatarUrl());
        mSize.setText(Data.formatKB(repo.getSize()));
        mIssues.setText(Integer.toString(repo.getIssues()));
        mForks.setText(Integer.toString(repo.getForks()));
        mWatchers.setText(Integer.toString(repo.getWatchers()));
        mStars.setText(Integer.toString(repo.getStarGazers()));
        mRefresher.setRefreshing(true);
        mLoader.loadProjects(this, mRepo.getFullName());
        mLoader.loadReadMe(this, mRepo.getFullName());
        if(mRepo.getUserLogin().equals(GitHubSession.getSession(this).getUserLogin())) {
            mAdapter.enableEditAccess();
            mAccessLevel = Repository.AccessLevel.ADMIN;
            findViewById(R.id.repo_new_project_card).setVisibility(View.VISIBLE);
        } else {
            mLoader.checkAccessToRepository(new Loader.AccessCheckListener() {
                @Override
                public void accessCheckComplete(Repository.AccessLevel level) {
                    Log.i(TAG, "accessCheckComplete: Access " + level);
                    mAccessLevel = level;
                    if(mAccessLevel == Repository.AccessLevel.ADMIN || mAccessLevel == Repository.AccessLevel.WRITE) {
                        mAdapter.enableEditAccess();
                        findViewById(R.id.repo_new_project_card).setVisibility(View.VISIBLE);
                    }
//                    if(canAccess) mAdapter.enableEditAccess();
//                    Toast.makeText(RepoActivity.this,
//                            canAccess ? R.string.text_can_access_repo : R.string.text_cannot_access_repo,
//                            Toast.LENGTH_SHORT)
//                            .show();
                }

                @Override
                public void accessCheckError(APIHandler.APIError error) {
                    mAccessLevel = Repository.AccessLevel.NONE;
                }
            }, GitHubSession.getSession(this).getUserLogin(), mRepo.getFullName());
        }
        mLoader.checkIfStarred(isStarred -> {
            mHasStarredRepo = isStarred;
            if(isStarred) {
                ((TextView) findViewById(R.id.repo_stars_text)).setText(R.string.text_unstar);
            } else {
                ((TextView) findViewById(R.id.repo_stars_text)).setText(R.string.text_star);
            }
        }, mRepo.getFullName());
        mLoader.checkIfWatched(isWatching -> {
            mIsWatchingRepo = isWatching;
            if(isWatching) {
                ((TextView) findViewById(R.id.repo_watchers_text)).setText(R.string.text_unwatch);
            } else {
                ((TextView) findViewById(R.id.repo_watchers_text)).setText(R.string.text_watch);
            }
        }, mRepo.getFullName());
    }

    @Override
    public void repoLoadError(APIHandler.APIError error) {

    }

    @Override
    public void openProject(Project project, View name) {
        final Intent i = new Intent(RepoActivity.this, ProjectActivity.class);
        i.putExtra(getString(R.string.parcel_project), project);
        i.putExtra(getString(R.string.intent_access_level), mAccessLevel);
        startActivity(i,
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this,
                        name,
                        getString(R.string.transition_title)
                ).toBundle()
        );
    }

    @Override
    public void editProject(Project project) {
        final ProjectDialog dialog = new ProjectDialog();
        final Bundle bundle = new Bundle();
        bundle.putParcelable(getString(R.string.parcel_project), project);
        dialog.setArguments(bundle);
        dialog.setListener(this);
        dialog.show(getSupportFragmentManager(), TAG);
    }

    @Override
    public void deleteProject(final Project project, Editor.ProjectDeletionListener listener) {
        Log.i(TAG, "deleteProject: Deleting project");
        final Dialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.text_delete_project)
                .setMessage(R.string.text_delete_project_warning)
                .setPositiveButton(R.string.action_ok, (dialogInterface, i) -> mEditor.deleteProject(new Editor.ProjectDeletionListener() {
                    @Override
                    public void projectDeleted(Project project1) {
                        final Bundle bundle = new Bundle();
                        bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                        mAnalytics.logEvent(Analytics.TAG_PROJECT_DELETION, bundle);
                        listener.projectDeleted(project1);
                    }

                    @Override
                    public void projectDeletionError(APIHandler.APIError error) {
                        final Bundle bundle = new Bundle();
                        bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                        mAnalytics.logEvent(Analytics.TAG_PROJECT_DELETION, bundle);
                    }
                }, project))
                .setNegativeButton(R.string.action_cancel, (dialogInterface, i) -> {

                }).create();
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.show();
    }

    @Override
    public void projectEditDone(Project project, boolean isNewProject) {
        if(isNewProject) {
            mEditor.createProject(this, project, mRepo.getFullName());
        } else {
            mEditor.editProject(new Editor.ProjectEditListener() {
                @Override
                public void projectEdited(Project project) {
                    Toast.makeText(RepoActivity.this, R.string.text_project_edited, Toast.LENGTH_LONG).show();
                    mAdapter.updateProject(project);

                    final Bundle bundle = new Bundle();
                    bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                    mAnalytics.logEvent(Analytics.TAG_PROJECT_EDIT, bundle);
                }

                @Override
                public void projectEditError(APIHandler.APIError error) {
                    final Bundle bundle = new Bundle();
                    bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                    mAnalytics.logEvent(Analytics.TAG_PROJECT_EDIT, bundle);
                }
            }, project);
        }
    }

    @Override
    public void projectEditCancelled() {

    }

    @Override
    public void projectCreated(Project project) {
        Toast.makeText(RepoActivity.this, R.string.text_project_created, Toast.LENGTH_LONG).show();
        mAdapter.addProject(project);

        final Bundle bundle = new Bundle();
        bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
        mAnalytics.logEvent(Analytics.TAG_PROJECT_CREATION, bundle);

    }

    @Override
    public void projectCreationError(APIHandler.APIError error) {
        final Bundle bundle = new Bundle();
        bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
        mAnalytics.logEvent(Analytics.TAG_PROJECT_CREATION, bundle);
    }

    @Override
    public void projectsLoaded(Project[] projects) {
        mRefresher.setRefreshing(false);
        mAdapter.projectsLoaded(projects);

        final Bundle bundle = new Bundle();
        bundle.putInt(Analytics.KEY_PROJECT_COUNT, projects.length);
        mAnalytics.logEvent(Analytics.TAG_REPO_ACTIVITY, bundle);
    }

    @Override
    public void projectsLoadError(APIHandler.APIError error) {

    }

    @Override
    public void readMeLoaded(String readMe) {
        Log.i(TAG, "readMeLoaded: ");
        mReadmeButton.setVisibility(View.VISIBLE);
        mReadme.setMDText(readMe);
        mReadme.reload();

    }

    @Override
    public void readmeLoadError(APIHandler.APIError error) {

    }

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        mReadme.setVisibility(GONE);
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_settings:
                startActivity(new Intent(RepoActivity.this, SettingsActivity.class));
                break;
            case R.id.menu_source:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL)));
                break;
            case R.id.menu_share:
                final Intent share = new Intent();
                share.setAction(Intent.ACTION_SEND);
                share.putExtra(Intent.EXTRA_TEXT, mRepo.getHtmlUrl());
                share.setType("text/plain");
                startActivity(share);
                break;
            case R.id.menu_save_to_homescreen:
                final ShortcutDialog dialog = new ShortcutDialog();
                final Bundle args = new Bundle();
                args.putInt(getString(R.string.intent_title_res), R.string.title_save_repository_shortcut);
                args.putString(getString(R.string.intent_name), mRepo.getName());

                dialog.setArguments(args);
                dialog.setListener((name, iconFlag) -> {
                    final Intent i = new Intent(getApplicationContext(), RepoActivity.class);
                    i.putExtra(getString(R.string.intent_repo), mRepo.getFullName());

                    final Intent add = new Intent();
                    add.putExtra(Intent.EXTRA_SHORTCUT_INTENT, i);
                    add.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
                    add.putExtra("duplicate", false);
                    add.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.mipmap.ic_launcher));
                    add.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                    getApplicationContext().sendBroadcast(add);
                });
                dialog.show(getSupportFragmentManager(), TAG);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAnalytics.setAnalyticsCollectionEnabled(SettingsActivity.Preferences.getPreferences(this).areAnalyticsEnabled());
    }

}
