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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.ShareActionProvider;
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
import com.tpb.projects.data.Editor;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.auth.GitHubSession;
import com.tpb.projects.data.models.Project;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.project.ProjectActivity;
import com.tpb.projects.user.SettingsActivity;
import com.tpb.projects.util.Analytics;
import com.tpb.projects.util.Constants;
import com.tpb.projects.util.Data;

import butterknife.BindView;
import butterknife.ButterKnife;

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
    private ShareActionProvider mShareActionProvider;

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
    private Repository mRepo;
    private boolean mCanAccess;

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
            //TODO Begin loading repo from url
        }
    }

    @Override
    public void openProject(Project project, View name) {
        final Intent i = new Intent(RepoActivity.this, ProjectActivity.class);
        i.putExtra(getString(R.string.parcel_project), project);
        i.putExtra(getString(R.string.intent_can_edit), mCanAccess);
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
        new AlertDialog.Builder(this)
                .setTitle(R.string.text_delete_project)
                .setMessage(R.string.text_delete_project_warning)
                .setPositiveButton(R.string.action_ok, (dialogInterface, i) -> new Editor(RepoActivity.this).deleteProject(new Editor.ProjectDeletionListener() {
                    @Override
                    public void projectDeleted(Project project1) {
                        final Bundle bundle = new Bundle();
                        bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                        mAnalytics.logEvent(Analytics.TAG_PROJECT_DELETION, bundle);
                        listener.projectDeleted(project1);
                    }

                    @Override
                    public void projectDeletionError() {
                        final Bundle bundle = new Bundle();
                        bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                        mAnalytics.logEvent(Analytics.TAG_PROJECT_DELETION, bundle);
                    }
                }, project))
                .setNegativeButton(R.string.action_cancel, (dialogInterface, i) -> {

                }).show();

    }

    @Override
    public void projectEditDone(Project project, boolean isNewProject) {
        if(isNewProject) {
            new Editor(this).createProject(this, project, mRepo.getFullName());
        } else {
            new Editor(this).editProject(new Editor.ProjectEditListener() {
                @Override
                public void projectEdited(Project project) {
                    Toast.makeText(RepoActivity.this, R.string.text_project_edited, Toast.LENGTH_LONG).show();
                    mAdapter.updateProject(project);

                    final Bundle bundle = new Bundle();
                    bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                    mAnalytics.logEvent(Analytics.TAG_PROJECT_EDIT, bundle);
                }

                @Override
                public void projectEditError() {
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
    public void projectCreationError() {
        final Bundle bundle = new Bundle();
        bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
        mAnalytics.logEvent(Analytics.TAG_PROJECT_CREATION, bundle);
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
        if(mRepo.getUserLogin().equals(GitHubSession.getSession(this).getUsername())) {
            mAdapter.enableRepoAccess();
            mCanAccess = true;
        } else {
            mLoader.checkAccess(new Loader.AccessCheckListener() {
                @Override
                public void accessCheckComplete(boolean canAccess) {
                    Log.i(TAG, "accessCheckComplete: Aaccess " + canAccess);
                    mCanAccess = canAccess;
                    if(canAccess) mAdapter.enableRepoAccess();
                    Toast.makeText(RepoActivity.this,
                            canAccess ? R.string.text_can_access_repo : R.string.text_cannot_access_repo,
                            Toast.LENGTH_SHORT)
                            .show();
                }

                @Override
                public void accessCheckError() {

                }
            }, GitHubSession.getSession(this).getUsername(), mRepo.getFullName());
        }
    }

    @Override
    public void repoLoadError() {

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
    public void readMeLoaded(String readMe) {
        Log.i(TAG, "readMeLoaded: ");
        mReadmeButton.setVisibility(View.VISIBLE);
        mReadme.setMDText(readMe);
        mReadme.reload();

        //TODO Dark theming
    }

    @Override
    public void readmeLoadError() {

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
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menu.findItem(R.id.menu_share));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_settings) {
            startActivity(new Intent(RepoActivity.this, SettingsActivity.class));
        } else if(item.getItemId() == R.id.menu_source) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL)));
        } else if(item.getItemId() == R.id.menu_share) {
            mShareActionProvider.setShareIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(mRepo.getHtmlUrl())));
        }

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAnalytics.setAnalyticsCollectionEnabled(SettingsActivity.Preferences.getPreferences(this).areAnalyticsEnabled());
    }

}
