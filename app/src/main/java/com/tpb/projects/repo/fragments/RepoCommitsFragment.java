package com.tpb.projects.repo.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.tpb.animatingrecyclerview.AnimatingRecyclerView;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.repo.RepoActivity;
import com.tpb.projects.repo.RepoCommitsAdapter;
import com.tpb.projects.util.fab.FloatingActionButton;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by theo on 29/03/17.
 */

public class RepoCommitsFragment extends RepoFragment implements Loader.ListLoader<Pair<String, String>> {

    private Unbinder unbinder;

    @BindView(R.id.repo_commits_branch_spinner) Spinner mBranchSpinner;
    @BindView(R.id.repo_commits_recycler) AnimatingRecyclerView mRecyclerView;
    @BindView(R.id.repo_commits_refresher) SwipeRefreshLayout mRefresher;

    private RepoCommitsAdapter mAdapter;
    private List<Pair<String, String>> mBranches;

    public static RepoCommitsFragment newInstance(RepoActivity parent) {
        final RepoCommitsFragment rcf = new RepoCommitsFragment();
        rcf.mParent = parent;
        return rcf;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_repo_commits, container, false);
        unbinder = ButterKnife.bind(this, view);
        mRefresher.setRefreshing(true);
        mAdapter = new RepoCommitsAdapter(this, mRefresher);
        final LinearLayoutManager manager = new LinearLayoutManager(getContext());
        mRecyclerView.enableLineDecoration();
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(manager.findFirstVisibleItemPosition() + 20 > manager.getItemCount()) {
                    mAdapter.notifyBottomReached();
                }
            }
        });
        mAreViewsValid = true;
        if(mRepo != null) repoLoaded(mRepo);
        if(mBranches != null) listLoadComplete(mBranches);
        mParent.notifyFragmentViewCreated(this);
        return view;
    }

    @Override
    public void repoLoaded(Repository repo) {
        mRepo = repo;
        new Loader(getContext()).loadBranches(this, mRepo.getFullName());
        if(!mAreViewsValid) return;
        mAdapter.setRepo(mRepo);
    }



    @Override
    public void listLoadComplete(List<Pair<String, String>> branches) {
        mBranches = branches;
        if(!mAreViewsValid) return;
        final String[] branchNames = new String[mBranches.size()];
        for(int i = 0; i < mBranches.size(); i++) {
            branchNames[i] = mBranches.get(i).first;
        }
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, branchNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBranchSpinner.setAdapter(adapter);
    }

    public void bindBranches(String sha) {

    }

    @Override
    public void listLoadError(APIHandler.APIError error) {

    }

    @Override
    public void handleFab(FloatingActionButton fab) {
        fab.hide(true);
    }

    @Override
    public void notifyBackPressed() {

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
