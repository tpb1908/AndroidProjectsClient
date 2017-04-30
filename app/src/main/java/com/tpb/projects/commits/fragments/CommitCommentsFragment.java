package com.tpb.projects.commits.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.tpb.github.data.APIHandler;
import com.tpb.github.data.Editor;
import com.tpb.github.data.auth.GitHubSession;
import com.tpb.github.data.models.Comment;
import com.tpb.github.data.models.Commit;
import com.tpb.projects.R;
import com.tpb.projects.commits.CommitCommentsAdapter;
import com.tpb.projects.common.FixedLinearLayoutManger;
import com.tpb.projects.common.fab.FloatingActionButton;
import com.tpb.projects.editors.CommentEditor;
import com.tpb.projects.flow.IntentHandler;
import com.tpb.projects.util.UI;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by theo on 30/03/17.
 */

public class CommitCommentsFragment extends CommitFragment {

    private Unbinder unbinder;

    @BindView(R.id.fragment_recycler) RecyclerView mRecycler;
    @BindView(R.id.fragment_refresher) SwipeRefreshLayout mRefresher;
    private FloatingActionButton mFab;

    private Editor mEditor;

    private CommitCommentsAdapter mAdapter;

    public static CommitCommentsFragment getInstance() {
        return new CommitCommentsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_recycler, container, false);
        unbinder = ButterKnife.bind(this, view);
        mEditor = Editor.getEditor(getContext());
        mAdapter = new CommitCommentsAdapter(this, mRefresher);
        mRecycler.setLayoutManager(new FixedLinearLayoutManger(getContext()));
        mRecycler.setAdapter(mAdapter);

        mAreViewsValid = true;
        if(mFab != null) addListeners();
        if(mCommit != null) commitLoaded(mCommit);
        return view;
    }

    public void setFab(FloatingActionButton fab) {
        mFab = fab;
        if(mAreViewsValid) addListeners();
    }

    private void addListeners() {
        final LinearLayoutManager manager = (LinearLayoutManager) mRecycler.getLayoutManager();
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
        mFab.setOnClickListener(v -> {
            final Intent i = new Intent(getContext(), CommentEditor.class);
            UI.setViewPositionForIntent(i, mFab);
            startActivityForResult(i, CommentEditor.REQUEST_CODE_NEW_COMMENT);
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if(mFab != null && mAreViewsValid) addListeners();
    }

    @Override
    public void commitLoaded(Commit commit) {
        mCommit = commit;
        if(!areViewsValid()) return;
        mAdapter.setCommit(mCommit);
    }

    private void createComment(Comment comment) {
        mRefresher.setRefreshing(true);
        mEditor.createCommitComment(new Editor.CreationListener<Comment>() {
            @Override
            public void created(Comment comment) {
                mRefresher.setRefreshing(false);
                mAdapter.addComment(comment);
                mRecycler.post(() -> mRecycler.smoothScrollToPosition(mAdapter.getItemCount()));
            }

            @Override
            public void creationError(APIHandler.APIError error) {
                mRefresher.setRefreshing(false);
            }
        }, mCommit.getFullRepoName(), mCommit.getSha(), comment.getBody());
    }

    private void editComment(Comment comment) {
        mRefresher.setRefreshing(true);
        mEditor.updateCommitComment(new Editor.UpdateListener<Comment>() {
            @Override
            public void updated(Comment comment) {
                mRefresher.setRefreshing(false);
                mAdapter.updateComment(comment);
            }

            @Override
            public void updateError(APIHandler.APIError error) {
                mRefresher.setRefreshing(false);
            }
        }, mCommit.getFullRepoName(), comment.getId(), comment.getBody());
    }

    void removeComment(Comment comment) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.title_delete_comment);
        builder.setPositiveButton(R.string.action_yes, (dialogInterface, i) -> {
            mRefresher.setRefreshing(true);
            mEditor.deleteCommitComment(new Editor.DeletionListener<Integer>() {
                @Override
                public void deleted(Integer id) {
                    mRefresher.setRefreshing(false);
                    mAdapter.removeComment(id);
                }

                @Override
                public void deletionError(APIHandler.APIError error) {
                    mRefresher.setRefreshing(false);
                }
            }, mCommit.getFullRepoName(), comment.getId());
        });
        builder.setNegativeButton(R.string.action_no, null);
        final Dialog deleteDialog = builder.create();
        deleteDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        deleteDialog.show();
    }

    public void displayCommentMenu(View view, Comment comment) {
        final PopupMenu menu = new PopupMenu(getContext(), view);
        menu.inflate(R.menu.menu_comment);
        if(comment.getUser().getLogin().equals(
                GitHubSession.getSession(getContext()).getUserLogin())) {
            menu.getMenu()
                .add(0, R.id.menu_edit_comment, Menu.NONE, getString(R.string.menu_edit_comment));
            menu.getMenu().add(0, R.id.menu_delete_comment, Menu.NONE,
                    getString(R.string.menu_delete_comment)
            );
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
                    removeComment(comment);
                    break;
                case R.id.menu_copy_comment_text:
                    final ClipboardManager cm = (ClipboardManager) getActivity().getSystemService(
                            Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("Comment", comment.getBody()));
                    Toast.makeText(getContext(), getString(R.string.text_copied_to_board),
                            Toast.LENGTH_SHORT
                    ).show();
                    break;
                case R.id.menu_fullscreen:
                    IntentHandler.showFullScreen(getContext(), comment.getBody(),
                            mCommit.getFullRepoName(), getFragmentManager()
                    );
                    break;
            }
            return false;
        });
        menu.show();
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
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
