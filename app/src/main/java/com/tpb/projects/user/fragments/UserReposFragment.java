package com.tpb.projects.user.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tpb.animatingrecyclerview.AnimatingRecycler;
import com.tpb.projects.R;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.data.models.State;
import com.tpb.projects.data.models.User;
import com.tpb.projects.repo.RepoActivity;
import com.tpb.projects.user.RepositoriesAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by theo on 10/03/17.
 */

public class UserReposFragment extends UserFragment implements RepositoriesAdapter.RepoOpener{

    private Unbinder unbinder;

    @BindView(R.id.user_repos_recycler) AnimatingRecycler mRecycler;
    @BindView(R.id.user_repos_refresher) SwipeRefreshLayout mRefresher;
    private RepositoriesAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_user_repos, container, false);
        unbinder = ButterKnife.bind(this, view);

        final LinearLayoutManager manager = new LinearLayoutManager(getContext());
        mRecycler.setLayoutManager(manager);
        mAdapter = new RepositoriesAdapter(getContext(), this, mRefresher);
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
        return view;
    }

    @Override
    public void userLoaded(User user) {
        mAdapter.setUser(user.getLogin(), false);
    }

    @Override
    public void openRepo(Repository repo, View view) {
        final Intent i = new Intent(getContext(), RepoActivity.class);
        i.putExtra(getString(R.string.intent_repo), repo);
        new Loader(getContext()).loadProjects(null, repo.getFullName());
        new Loader(getContext()).loadIssues(null, repo.getFullName(), State.OPEN, null, null, 0);
        startActivity(i, ActivityOptionsCompat.makeSceneTransitionAnimation(
                getActivity(),
                view,
                getString(R.string.transition_name)
                ).toBundle()
        );
        getActivity().overridePendingTransition(R.anim.slide_up, R.anim.none);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
