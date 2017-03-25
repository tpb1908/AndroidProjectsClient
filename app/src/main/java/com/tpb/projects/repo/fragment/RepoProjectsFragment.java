package com.tpb.projects.repo.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tpb.projects.R;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.repo.RepoProjectsAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by theo on 25/03/17.
 */

public class RepoProjectsFragment extends RepoFragment {

    private Unbinder unbinder;

    @BindView(R.id.repo_projects_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.repo_projects_recycler) RecyclerView mRecycler;
    private RepoProjectsAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_repo_projects, container, false);
        unbinder = ButterKnife.bind(this, view);
        mAdapter = new RepoProjectsAdapter(this, mRefresher);
        mRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecycler.setAdapter(mAdapter);
        mRefresher.setOnRefreshListener(() -> mAdapter.reload());
        return view;
    }

    @Override
    public void repoLoaded(Repository repo) {
        mAdapter.setRepository(repo);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
