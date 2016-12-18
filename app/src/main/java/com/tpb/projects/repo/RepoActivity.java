package com.tpb.projects.repo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.androidnetworking.widget.ANImageView;
import com.mittsu.markedview.MarkedView;
import com.tpb.projects.R;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.auth.models.Project;
import com.tpb.projects.data.auth.models.Repository;
import com.tpb.projects.util.Constants;
import com.tpb.projects.util.Data;
import com.tpb.projects.views.AnimatingRecycler;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.view.View.GONE;

/**
 * Created by theo on 16/12/16.
 */

public class RepoActivity extends AppCompatActivity implements Loader.RepositoryLoader, Loader.ReadMeLoader, Loader.ProjectsLoader, ProjectAdapter.ProjectEditor {
    private static final String TAG = RepoActivity.class.getSimpleName();

    @BindView(R.id.repo_name) TextView mName;
    @BindView(R.id.repo_description) TextView mDescription;
    @BindView(R.id.user_image) ANImageView mUserImage;
    @BindView(R.id.user_name) TextView mUserName;

    @BindView(R.id.repo_forks) TextView mForks;
    @BindView(R.id.repo_size) TextView mSize;
    @BindView(R.id.repo_stars) TextView mStars;
    @BindView(R.id.repo_watchers) TextView mWatchers;

    @BindView(R.id.repo_show_readme) Button mReadmeButton;
    @BindView(R.id.repo_readme) MarkedView mReadme;

    @BindView(R.id.repo_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.repo_project_recycler) AnimatingRecycler mRecycler;

    @BindView(R.id.repo_new_project) Button mNewProjectButton;

    private ProjectAdapter mAdapter;
    private Loader mLoader;
    private Repository mRepo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme_Dark);
        setContentView(R.layout.activity_repo);
        ButterKnife.bind(this);
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
            new ProjectDialog().show(getSupportFragmentManager(), TAG);
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
        mAdapter = new ProjectAdapter(this);
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
    public void editProject(Project project) {
        final ProjectDialog dialog = new ProjectDialog();
        final Bundle bundle = new Bundle();
        bundle.putParcelable(getString(R.string.parcel_project), project);
        dialog.setArguments(bundle);
        dialog.show(getSupportFragmentManager(), TAG);
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
        mForks.setText(Integer.toString(repo.getForks()));
        mWatchers.setText(Integer.toString(repo.getWatchers()));
        mStars.setText(Integer.toString(repo.getStarGazers()));
        mRefresher.setRefreshing(true);
        mLoader.loadProjects(this, mRepo.getFullName());
        mLoader.loadReadMe(this, mRepo.getFullName());
    }

    @Override
    public void projectsLoaded(Project[] projects) {
        mRefresher.setRefreshing(false);
        mAdapter.projectsLoaded(projects);
    }

    @Override
    public void readMeLoaded(String readMe) {
        Log.i(TAG, "readMeLoaded: ");
        mReadmeButton.setVisibility(View.VISIBLE);
        mReadme.setMDText(readMe);
        //TODO Dark theming
    }

    @Override
    public void loadError() {

    }

    @Override
    public void onBackPressed() {
        mReadme.setVisibility(GONE);
        super.onBackPressed();
    }
}
