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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.tpb.animatingrecyclerview.AnimatingRecyclerView;
import com.tpb.github.data.APIHandler;
import com.tpb.github.data.Loader;
import com.tpb.github.data.models.Repository;
import com.tpb.projects.R;
import com.tpb.projects.common.fab.FloatingActionButton;
import com.tpb.projects.repo.RepoCommitsAdapter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by theo on 29/03/17.
 */

public class RepoCommitsFragment extends RepoFragment implements Loader.ListLoader<Pair<String, String>> {
    private static final String TAG = RepoCommitsFragment.class.getSimpleName();

    private Unbinder unbinder;

    @BindView(R.id.repo_commits_branch_spinner) Spinner mBranchSpinner;
    @BindView(R.id.repo_commits_recycler) AnimatingRecyclerView mRecyclerView;
    @BindView(R.id.repo_commits_refresher) SwipeRefreshLayout mRefresher;

    private RepoCommitsAdapter mAdapter;
    private List<Pair<String, String>> mBranches;
    private boolean mIsLoadingBranches = false;
    private String mLatestSHA;

    public static RepoCommitsFragment newInstance() {
        return new RepoCommitsFragment();
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
        return view;
    }

    @Override
    public void repoLoaded(Repository repo) {
        mRepo = repo;
        Loader.getLoader(getContext()).loadBranches(this, mRepo.getFullName());
        mIsLoadingBranches = true;
        if(!areViewsValid()) return;
        mAdapter.setRepo(mRepo);
    }

    @Override
    public void listLoadComplete(List<Pair<String, String>> branches) {
        mIsLoadingBranches = false;
        mBranches = branches;
        if(!areViewsValid()) return;
        if(mLatestSHA != null) bindBranches();
    }

    public void setLatestSHA(String sha) {
        if(mLatestSHA == null) {
            mLatestSHA = sha;
            if(mBranches != null && !mBranches.isEmpty()) {
                bindBranches();
            } else if(!mIsLoadingBranches) {
                Loader.getLoader(getContext()).loadBranches(this, mRepo.getFullName());
            }
        }
    }

    public void bindBranches() {
        final List<String> branchNames = new ArrayList<>(mBranches.size());
        for(Pair<String, String> p : mBranches) {
            if(mLatestSHA.equals(p.second)) {
                branchNames.add(0, p.first);
            } else {
                branchNames.add(p.first);
            }
        }
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, branchNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBranchSpinner.setAdapter(adapter);
        if(mBranchSpinner.getOnItemSelectedListener() == null) {
            mBranchSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    mAdapter.setBranch(branchNames.get(position));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }
    }

    @Override
    public void listLoadError(APIHandler.APIError error) {
        mIsLoadingBranches = false;
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
