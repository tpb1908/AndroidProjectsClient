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
import com.tpb.projects.R;
import com.tpb.projects.data.models.Gist;
import com.tpb.projects.data.models.User;
import com.tpb.projects.repo.content.FileActivity;
import com.tpb.projects.user.GistsAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by theo on 10/03/17.
 */

public class UserGistsFragment extends UserFragment implements GistsAdapter.GistOpener {

    private Unbinder unbinder;

    @BindView(R.id.user_gists_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.user_gists_recycler) AnimatingRecyclerView mRecycler;

    private GistsAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_user_gists, container, false);
        unbinder = ButterKnife.bind(this, view);

        final LinearLayoutManager manager = new LinearLayoutManager(getContext());
        mRecycler.setLayoutManager(manager);
        mRecycler.enableLineDecoration();
        mAdapter = new GistsAdapter(getContext(), this, mRefresher);
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
        mUser = user;
    }

    @Override
    public void openGist(Gist gist, View view) {
        final Intent i = new Intent(getContext(), FileActivity.class);
        i.putExtra(getString(R.string.intent_gist_url), gist.getFiles()[0].getRawUrl());
        startActivity(i);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
