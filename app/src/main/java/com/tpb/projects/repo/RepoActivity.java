package com.tpb.projects.repo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.LayoutInflaterCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import com.androidnetworking.widget.ANImageView;
import com.mikepenz.iconics.context.IconicsLayoutInflater;
import com.tpb.projects.R;

import butterknife.BindView;
import us.feras.mdv.MarkdownView;

/**
 * Created by theo on 16/12/16.
 */

public class RepoActivity extends AppCompatActivity {


    @BindView(R.id.repo_name) TextView mName;
    @BindView(R.id.repo_description) TextView mDescription;
    @BindView(R.id.user_image) ANImageView mUserImage;
    @BindView(R.id.user_name) TextView mUserName;
    @BindView(R.id.user_id) TextView mUserId;

    @BindView(R.id.repo_commits) TextView mCommits;
    @BindView(R.id.repo_forks) TextView mForks;
    @BindView(R.id.repo_size) TextView mSize;

    @BindView(R.id.repo_readme) MarkdownView mReadMe;

    @BindView(R.id.repo_project_recycler) RecyclerView mRecycler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        LayoutInflaterCompat.setFactory(getLayoutInflater(), new IconicsLayoutInflater(getDelegate()));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repo);
    }
}
