package com.tpb.projects.commits.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.tpb.projects.R;
import com.tpb.projects.commits.CommitCommentsAdapter;
import com.tpb.projects.data.auth.GitHubSession;
import com.tpb.projects.data.models.Comment;
import com.tpb.projects.data.models.Commit;
import com.tpb.projects.editors.CommentEditor;
import com.tpb.projects.util.FixedLinearLayoutManger;
import com.tpb.projects.util.UI;
import com.tpb.projects.util.fab.FloatingActionButton;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by theo on 30/03/17.
 */

public class CommitCommentsFragment extends CommitFragment {

    private Unbinder unbinder;

    @BindView(R.id.commit_comments_recycler) RecyclerView mRecycler;
    @BindView(R.id.commit_comments_refresher) SwipeRefreshLayout mRefresher;
    private FloatingActionButton mFab;

    private CommitCommentsAdapter mAdapter;

    public static CommitCommentsFragment getInstance(FloatingActionButton fab) {
        final CommitCommentsFragment ccf = new CommitCommentsFragment();
        ccf.mFab = fab;
        return ccf;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_commit_comments, container, false);
        unbinder = ButterKnife.bind(this, view);
        final LinearLayoutManager manager = new FixedLinearLayoutManger(getContext());
        mAdapter = new CommitCommentsAdapter(this, mRefresher);
        mRecycler.setLayoutManager(manager);
        mRecycler.setAdapter(mAdapter);
        mRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(manager.findFirstVisibleItemPosition() + 20 > manager.getItemCount()) {
                    mAdapter.notifyBottomReached();
                }
                if(dy > 10) {
                    mFab.hide(true);
                } else if(dy < -10) {
                    mFab.show(true);
                }
            }
        });
        mAreViewsValid = true;
        if(mCommit != null) commitLoaded(mCommit);
        return view;
    }

    @Override
    public void commitLoaded(Commit commit) {
        mCommit = commit;
        if(!mAreViewsValid) return;
        mAdapter.setCommit(mCommit);
    }

    public void displayCommentMenu(View view, Comment comment) {
        final PopupMenu menu = new PopupMenu(getContext(), view);
        menu.inflate(R.menu.menu_comment);
        if(comment.getUser().getLogin().equals(
                GitHubSession.getSession(getContext()).getUserLogin())) {
            menu.getMenu().add(0, R.id.menu_edit_comment, Menu.NONE, getString(R.string.menu_edit_comment));
            menu.getMenu().add(0, R.id.menu_delete_comment, Menu.NONE, getString(R.string.menu_delete_comment));
        }
        menu.setOnMenuItemClickListener(menuItem -> {
            switch(menuItem.getItemId()) {
                case R.id.menu_edit_comment:
                    final Intent i = new Intent(getContext(), CommentEditor.class);
                    i.putExtra(getString(R.string.parcel_comment), comment);
                    UI.setViewPositionForIntent(i, view);
                    startActivityForResult(i, CommentEditor.REQUEST_CODE_EDIT_COMMENT);
                    break;
                case R.id.menu_delete_comment:
                    //removeComment(comment);
                    break;
                case R.id.menu_copy_comment_text:
                    final ClipboardManager cm = (ClipboardManager) getActivity().getSystemService(
                            Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("Comment", comment.getBody()));
                    Toast.makeText(getContext(), getString(R.string.text_copied_to_board), Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        });
        menu.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
