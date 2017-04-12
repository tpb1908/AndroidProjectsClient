package com.tpb.projects.user.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tpb.animatingrecyclerview.AnimatingRecyclerView;
import com.tpb.github.data.Loader;
import com.tpb.github.data.models.Repository;
import com.tpb.github.data.models.State;
import com.tpb.github.data.models.User;
import com.tpb.projects.R;
import com.tpb.projects.common.FixedLinearLayoutManger;
import com.tpb.projects.repo.RepoActivity;
import com.tpb.projects.user.RepositoriesAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by theo on 10/03/17.
 */

public class UserStarsFragment extends UserFragment implements RepositoriesAdapter.RepoOpener {

    private Unbinder unbinder;

    @BindView(R.id.fragment_recycler) AnimatingRecyclerView mRecycler;
    @BindView(R.id.fragment_refresher) SwipeRefreshLayout mRefresher;
    private RepositoriesAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_recycler, container, false);
        unbinder = ButterKnife.bind(this, view);

        final LinearLayoutManager manager = new FixedLinearLayoutManger(getContext());
        mRecycler.setLayoutManager(manager);
        mRecycler.enableLineDecoration();
        mAdapter = new RepositoriesAdapter(getActivity(), this, mRefresher);
        mRecycler.setAdapter(mAdapter);

        mRecycler.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(manager.findFirstVisibleItemPosition() + 20 > manager.getItemCount()) {
                    mAdapter.notifyBottomReached();
                }
            }
        });
        mAreViewsValid = true;
        if(mUser != null) userLoaded(mUser);
        return view;
    }


    @Override
    public void userLoaded(User user) {
        mUser = user;
        if(!mAreViewsValid) return;
        mAdapter.setUser(user.getLogin(), true);
    }

    @Override
    public void openRepo(Repository repo) {
        final Intent i = new Intent(getContext(), RepoActivity.class);
        i.putExtra(getString(R.string.intent_repo), repo);
        Loader.getLoader(getContext()).loadProjects(null, repo.getFullName());
        Loader.getLoader(getContext()).loadIssues(null, repo.getFullName(), State.OPEN, null, null, 0);
        startActivity(i);
        getActivity().overridePendingTransition(R.anim.slide_up, R.anim.none);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
