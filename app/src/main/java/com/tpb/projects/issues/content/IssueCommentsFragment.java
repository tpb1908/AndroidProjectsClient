package com.tpb.projects.issues.content;

import android.os.Bundle;
import android.support.annotation.Nullable;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tpb.projects.R;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.util.MultiOnRefreshListener;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by theo on 14/03/17.
 */

public class IssueCommentsFragment extends IssueFragment {

    private Unbinder unbinder;

    @BindView(R.id.issue_comments_recycler) RecyclerView mRecycler;

    private MultiOnRefreshListener mRefreshListener;
    private SwipeRefreshLayout mRefresher;

    private IssueCommentsAdapter mAdapter;

    public static IssueCommentsFragment getInstance(SwipeRefreshLayout refresher, MultiOnRefreshListener refreshListener) {
        final IssueCommentsFragment frag = new IssueCommentsFragment();
        frag.mRefreshListener = refreshListener;
        frag.mRefresher = refresher;
        return frag;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_issue_comments, container, false);
        unbinder = ButterKnife.bind(this, view);
        mAdapter = new IssueCommentsAdapter(this, mRefresher, mRefreshListener);
        final LinearLayoutManager manager = new LinearLayoutManager(getContext());
        mRecycler.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(manager.findFirstVisibleItemPosition() + 20 > manager.getItemCount()) {
                    mAdapter.notifyBottomReached();
                }
            }
        });
        mRecycler.setAdapter(mAdapter);
        return view;
    }

    @Override
    public void issueLoaded(Issue issue) {
        mAdapter.setIssue(issue);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
