package com.tpb.projects.repo;

import android.annotation.SuppressLint;
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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
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
import com.tpb.animatingrecyclerview.AnimatingRecyclerView;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Editor;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.auth.GitHubSession;
import com.tpb.projects.data.models.DataModel;
import com.tpb.projects.data.models.Project;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.editors.ProjectDialog;
import com.tpb.projects.issues.IssuesActivity;
import com.tpb.projects.project.ProjectActivity;
import com.tpb.projects.repo.content.ContentActivity;
import com.tpb.projects.user.UserActivity;
import com.tpb.projects.util.Analytics;
import com.tpb.projects.util.BaseActivity;
import com.tpb.projects.util.ShortcutDialog;
import com.tpb.projects.util.UI;
import com.tpb.projects.util.Util;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.view.View.GONE;

/**
 * Created by theo on 16/12/16.
 */

public class RepoActivity extends BaseActivity implements
        ProjectAdapter.ProjectEditor,
        ProjectDialog.ProjectListener,
        Editor.GITModelCreationListener<Project> {
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
    @BindView(R.id.repo_project_recycler) AnimatingRecyclerView mRecycler;

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
        UI.setStatusBarColor(getWindow(), getResources().getColor(R.color.colorPrimaryDark));
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
                mLoader.loadRepository(mRepoLoader, mRepo.getFullName());
            }
        });
        mAdapter = new ProjectAdapter(this, mRecycler);
        mRecycler.setAdapter(mAdapter);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        if(launchIntent.getParcelableExtra(getString(R.string.intent_repo)) != null) {
            mRepo = launchIntent.getParcelableExtra(getString(R.string.intent_repo));

            if(mRepo.isFork()) {
                mLoader.loadRepository(mRepoLoader, mRepo.getFullName());
            } else {
                mRepoLoader.loadComplete(launchIntent.getParcelableExtra(getString(R.string.intent_repo)));
            }
        } else {
            mLoader.loadRepository(mRepoLoader, launchIntent.getStringExtra(getString(R.string.intent_repo)));
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
            startActivity(i);
        }
    }

    @OnClick({R.id.repo_stars, R.id.repo_stars_text, R.id.repo_stars_drawable})
    void toggleStar() {
        final Editor.GITModelUpdateListener<Boolean> listener = new Editor.GITModelUpdateListener<Boolean>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void updated(Boolean isStarred) {
                mHasStarredRepo = isStarred;
                if(isStarred) {
                    ((TextView) findViewById(R.id.repo_stars_text)).setText(R.string.text_unstar);
                    mStars.setText(Integer.toString(Integer.parseInt(mStars.getText().toString()) + 1));
                } else {
                    ((TextView) findViewById(R.id.repo_stars_text)).setText(R.string.text_star);
                    mStars.setText(Integer.toString(Integer.parseInt(mStars.getText().toString()) - 1));
                }
            }

            @Override
            public void updateError(APIHandler.APIError error) {

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
        final Editor.GITModelUpdateListener<Boolean> listener = new Editor.GITModelUpdateListener<Boolean>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void updated(Boolean isWatched) {
                mIsWatchingRepo = isWatched;
                if(isWatched) {
                    ((TextView) findViewById(R.id.repo_watchers_text)).setText(R.string.text_unwatch);
                    mWatchers.setText(Integer.toString(Integer.parseInt(mWatchers.getText().toString()) + 1));
                } else {
                    ((TextView) findViewById(R.id.repo_stars_text)).setText(R.string.text_watch);
                    mWatchers.setText(Integer.toString(Integer.parseInt(mWatchers.getText().toString()) - 1));
                }
            }

            @Override
            public void updateError(APIHandler.APIError error) {

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
        if(mRepo != null) {
            final Intent i = new Intent(this, ContentActivity.class);
            i.putExtra(getString(R.string.intent_repo), mRepo.getFullName());
            startActivity(i,
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                            this,
                            mName,
                            getString(R.string.transition_name)
                    ).toBundle()
            );
        }

    }
    
    private Loader.GITModelLoader<Repository> mRepoLoader = new Loader.GITModelLoader<Repository>() {
        @SuppressLint("SetTextI18n")
        @Override
        public void loadComplete(Repository repo) {
            mRepo = repo;
            mName.setText(mRepo.getName());
            String desc = null;
            if(repo.isFork() && repo.getSource() != null) {
                //Tree of forks
                if(repo.getParent() != null && !repo.getSource().equals(repo.getParent())) {
                    desc = String.format(
                                getString(R.string.text_repo_forked_multiple),
                                String.format(
                                        getString(R.string.text_href),
                                        repo.getParent().getHtmlUrl(),
                                        repo.getParent().getFullName()
                                ),
                                String.format(getString(R.string.text_href),
                                        repo.getSource().getUrl(),
                                        repo.getSource().getFullName()
                                )
                        );
                } else {
                    desc = String.format(
                                getString(R.string.text_repo_forked),
                                String.format(
                                        getString(R.string.text_href),
                                        repo.getParent().getHtmlUrl(),
                                        repo.getParent().getFullName()
                                )
                    );
                }
            }
            if(desc == null && DataModel.JSON_NULL.equals(mRepo.getDescription())) {
                mDescription.setVisibility(GONE);
            } else {
                if(desc == null){
                    desc = repo.getDescription();
                } else {
                    desc += "<br>" + repo.getDescription();
                }
                mDescription.setVisibility(View.VISIBLE);
                mDescription.setText(Html.fromHtml(desc));
                mDescription.setMovementMethod(LinkMovementMethod.getInstance());
            }
            mUserName.setText(mRepo.getUserLogin());
            mUserImage.setImageUrl(mRepo.getUserAvatarUrl());
            mSize.setText(Util.formatKB(mRepo.getSize()));
            mIssues.setText(Integer.toString(mRepo.getIssues()));
            mForks.setText(Integer.toString(mRepo.getForks()));
            mWatchers.setText(Integer.toString(mRepo.getWatchers()));
            mStars.setText(Integer.toString(mRepo.getStarGazers()));
            mRefresher.setRefreshing(true);
            mLoader.loadProjects(mProjectsLoader, mRepo.getFullName());
            mLoader.loadReadMe(new Loader.GITModelLoader<String>() {
                @Override
                public void loadComplete(String data) {
                    Log.i(TAG, "readMeLoaded: ");
                    mReadmeButton.setVisibility(View.VISIBLE);
                    mReadme.setMDText(data);
                    mReadme.reload();
                }

                @Override
                public void loadError(APIHandler.APIError error) {

                }
            }, mRepo.getFullName());
            if(mRepo.getUserLogin().equals(GitHubSession.getSession(RepoActivity.this).getUserLogin())) {
                mAdapter.enableEditAccess();
                mAccessLevel = Repository.AccessLevel.ADMIN;
                findViewById(R.id.repo_new_project_card).setVisibility(View.VISIBLE);
            } else {
                mLoader.checkAccessToRepository(new Loader.GITModelLoader<Repository.AccessLevel>() {
                    @Override
                    public void loadComplete(Repository.AccessLevel data) {
                        mAccessLevel = data;
                        if(mAccessLevel == Repository.AccessLevel.ADMIN || mAccessLevel == Repository.AccessLevel.WRITE) {
                            mAdapter.enableEditAccess();
                            findViewById(R.id.repo_new_project_card).setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void loadError(APIHandler.APIError error) {
                        mAccessLevel = Repository.AccessLevel.NONE;
                    }
                }, GitHubSession.getSession(RepoActivity.this).getUserLogin(), mRepo.getFullName());
            }
            mLoader.checkIfStarred(new Loader.GITModelLoader<Boolean>() {
                @Override
                public void loadComplete(Boolean data) {
                    mHasStarredRepo = data;
                    if(mHasStarredRepo) {
                        ((TextView) findViewById(R.id.repo_stars_text)).setText(R.string.text_unstar);
                    } else {
                        ((TextView) findViewById(R.id.repo_stars_text)).setText(R.string.text_star);
                    }
                }

                @Override
                public void loadError(APIHandler.APIError error) {
                    mHasStarredRepo =  false;
                    ((TextView) findViewById(R.id.repo_stars_text)).setText(R.string.text_star);
                }
            }, mRepo.getFullName());
            mLoader.checkIfWatched(new Loader.GITModelLoader<Boolean>() {
                @Override
                public void loadComplete(Boolean data) {
                    mIsWatchingRepo = data;
                    if(mIsWatchingRepo) {
                        ((TextView) findViewById(R.id.repo_watchers_text)).setText(R.string.text_unwatch);
                    } else {
                        ((TextView) findViewById(R.id.repo_watchers_text)).setText(R.string.text_watch);
                    }
                }

                @Override
                public void loadError(APIHandler.APIError error) {
                    mIsWatchingRepo = false;
                    ((TextView) findViewById(R.id.repo_watchers_text)).setText(R.string.text_watch);
                }
            }, mRepo.getFullName());
        }

        @Override
        public void loadError(APIHandler.APIError error) {

        }
    };

    private Loader.GITModelsLoader<Project> mProjectsLoader = new Loader.GITModelsLoader<Project>() {
        @Override
        public void loadComplete(Project[] data) {
            mRefresher.setRefreshing(false);
            mAdapter.loadComplete(data);

            final Bundle bundle = new Bundle();
            bundle.putInt(Analytics.KEY_PROJECT_COUNT, data.length);
            mAnalytics.logEvent(Analytics.TAG_REPO_ACTIVITY, bundle);
        }

        @Override
        public void loadError(APIHandler.APIError error) {

        }
    };

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
    public void created(Project project) {
        Toast.makeText(RepoActivity.this, R.string.text_project_created, Toast.LENGTH_LONG).show();
        mAdapter.addProject(project);

        final Bundle bundle = new Bundle();
        bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
        mAnalytics.logEvent(Analytics.TAG_PROJECT_CREATION, bundle);
    }

    @Override
    public void creationError(APIHandler.APIError error) {
        final Bundle bundle = new Bundle();
        bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
        mAnalytics.logEvent(Analytics.TAG_PROJECT_CREATION, bundle);
    }

    @Override
    public void deleteProject(Project project, Editor.GITModelDeletionListener<Project> listener) {
        final Dialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.text_delete_project)
                .setMessage(R.string.text_delete_project_warning)
                .setPositiveButton(R.string.action_ok, (dialogInterface, i) -> mEditor.deleteProject(new Editor.GITModelDeletionListener<Project>() {
                    @Override
                    public void deleted(Project project) {
                        final Bundle bundle = new Bundle();
                        bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                        mAnalytics.logEvent(Analytics.TAG_PROJECT_DELETION, bundle);
                        listener.deleted(project);
                    }

                    @Override
                    public void deletionError(APIHandler.APIError error) {
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
            mEditor.updateProject(new Editor.GITModelUpdateListener<Project>() {
                @Override
                public void updated(Project project) {
                    Toast.makeText(RepoActivity.this, R.string.text_project_edited, Toast.LENGTH_LONG).show();
                    mAdapter.updateProject(project);

                    final Bundle bundle = new Bundle();
                    bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                    mAnalytics.logEvent(Analytics.TAG_PROJECT_EDIT, bundle);
                }

                @Override
                public void updateError(APIHandler.APIError error) {
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
