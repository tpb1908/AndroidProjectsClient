package com.tpb.projects.issues.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Editor;
import com.tpb.projects.data.models.Comment;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.editors.CommentEditor;
import com.tpb.projects.issues.IssueCommentsAdapter;
import com.tpb.projects.util.UI;
import com.tpb.projects.util.fab.FloatingActionButton;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by theo on 14/03/17.
 */

public class IssueCommentsFragment extends IssueFragment {

    private Unbinder unbinder;

    @BindView(R.id.issue_comments_recycler) RecyclerView mRecycler;
    @BindView(R.id.issue_comments_refresher) SwipeRefreshLayout mRefresher;
    private FloatingActionButton mFab;

    private IssueCommentsAdapter mAdapter;

    private Editor mEditor;

    private Repository.AccessLevel mAccessLevel;
    private Issue mIssue;

    public static IssueCommentsFragment getInstance(FloatingActionButton fab) {
        final IssueCommentsFragment frag = new IssueCommentsFragment();
        frag.mFab = fab;
        return frag;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEditor = new Editor(getContext());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_issue_comments, container, false);
        unbinder = ButterKnife.bind(this, view);
        final LinearLayoutManager manager = new LinearLayoutManager(getContext());
        mRecycler.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(manager.findFirstVisibleItemPosition() + 20 > manager.getItemCount()) {
                    mAdapter.notifyBottomReached();
                }
                mFab.show(true);
                if(dy > 10) {
                    mFab.hide(true);
                } else if(dy < -10) {
                    mFab.show(true);
                }
            }
        });
        mFab.setOnClickListener(v -> {
            final Intent i = new Intent(getContext(), CommentEditor.class);
            UI.setViewPositionForIntent(i, mFab);
            startActivityForResult(i, CommentEditor.REQUEST_CODE_NEW_COMMENT);
        });
        mAdapter = new IssueCommentsAdapter(this, mRefresher);
        if(mIssue != null) mAdapter.setIssue(mIssue);
        if(mAccessLevel != null && mAccessLevel == Repository.AccessLevel.ADMIN) {
            mFab.setVisibility(View.VISIBLE);
        }
        mRecycler.setAdapter(mAdapter);
        mRecycler.setLayoutManager(manager);
        mRefresher.setOnRefreshListener(() -> {
            mAdapter.clear();
            mAdapter.setIssue(mIssue);
        });

        return view;
    }

    @Override
    public void issueLoaded(Issue issue) {
        mIssue = issue;
        if(mAdapter != null) mAdapter.setIssue(issue);
    }

    @Override
    public void setAccessLevel(Repository.AccessLevel level) {
        mAccessLevel = level;
        if(mAccessLevel == Repository.AccessLevel.ADMIN) {
            mFab.setVisibility(View.VISIBLE);
            mFab.show(true);
        }
    }

    private void createComment(Comment comment) {
        mRefresher.setRefreshing(true);
        mEditor.createComment(new Editor.GITModelCreationListener<Comment>() {
            @Override
            public void created(Comment comment) {
                mRefresher.setRefreshing(false);
                mAdapter.addComment(comment);
                //mScrollView.post(() -> mScrollView.smoothScrollTo(0, mScrollView.getBottom()));
            }

            @Override
            public void creationError(APIHandler.APIError error) {
                mRefresher.setRefreshing(false);
            }
        }, mIssue.getRepoPath(), mIssue.getNumber(), comment.getBody());
    }

    public void createCommentForState(Comment comment) {
        createComment(comment);
    }

    private void editComment(Comment comment) {
        mEditor.updateComment(new Editor.GITModelUpdateListener<Comment>() {
            @Override
            public void updated(Comment comment) {
                mRefresher.setRefreshing(false);
                mAdapter.updateComment(comment);
            }

            @Override
            public void updateError(APIHandler.APIError error) {
                mRefresher.setRefreshing(false);
            }
        }, mIssue.getRepoPath(), comment.getId(), comment.getBody());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == AppCompatActivity.RESULT_OK) {
            final Comment comment = data.getParcelableExtra(getString(R.string.parcel_comment));
            if(requestCode == CommentEditor.REQUEST_CODE_NEW_COMMENT) {
                createComment(comment);
            } else if(requestCode == CommentEditor.REQUEST_CODE_EDIT_COMMENT) {
                editComment(comment);
            } else if(requestCode == CommentEditor.REQUEST_CODE_COMMENT_FOR_STATE) {
                createCommentForState(comment);
            }
        }
    }

    void removeComment(Comment comment) {

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
