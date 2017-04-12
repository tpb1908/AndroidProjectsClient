package com.tpb.projects.user.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tpb.animatingrecyclerview.AnimatingRecyclerView;
import com.tpb.github.data.models.User;
import com.tpb.projects.R;
import com.tpb.projects.common.FixedLinearLayoutManger;
import com.tpb.projects.user.UserAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by theo on 19/03/17.
 */

public class UserFollowersFragment extends UserFragment {

    private Unbinder unbinder;

    private UserAdapter mAdapter;
    @BindView(R.id.fragment_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.fragment_recycler) AnimatingRecyclerView mRecycler;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_recycler, container, false);
        unbinder = ButterKnife.bind(this, view);
        final LinearLayoutManager manager = new FixedLinearLayoutManger(getContext());
        mRecycler.setLayoutManager(manager);
        mAdapter = new UserAdapter(getActivity(), mRefresher);
        mRecycler.enableLineDecoration();
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
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
