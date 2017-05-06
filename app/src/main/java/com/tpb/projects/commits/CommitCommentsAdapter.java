package com.tpb.projects.commits;

import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.tpb.github.data.APIHandler;
import com.tpb.github.data.Loader;
import com.tpb.github.data.models.Comment;
import com.tpb.github.data.models.Commit;
import com.tpb.mdtext.Markdown;
import com.tpb.mdtext.imagegetter.HttpImageGetter;
import com.tpb.mdtext.views.MarkdownTextView;
import com.tpb.projects.R;
import com.tpb.projects.commits.fragments.CommitCommentsFragment;
import com.tpb.projects.common.NetworkImageView;
import com.tpb.projects.flow.IntentHandler;
import com.tpb.projects.markdown.Formatter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 01/04/17.
 */

public class CommitCommentsAdapter extends RecyclerView.Adapter<CommitCommentsAdapter.CommentHolder> implements Loader.ListLoader<Comment> {

    private final ArrayList<Pair<Comment, SpannableString>> mComments = new ArrayList<>();
    private Commit mCommit;
    private final CommitCommentsFragment mParent;

    private int mPage = 1;
    private boolean mIsLoading = false;
    private boolean mMaxPageReached = false;

    private SwipeRefreshLayout mRefresher;
    private Loader mLoader;

    public CommitCommentsAdapter(CommitCommentsFragment parent, SwipeRefreshLayout refresher) {
        mParent = parent;
        mRefresher = refresher;
        mLoader = Loader.getLoader(mParent.getContext());
        mRefresher.setOnRefreshListener(() -> {
            mPage = 1;
            mMaxPageReached = false;
            clear();
            loadComments(true);
        });
    }


    public void clear() {
        final int oldSize = mComments.size();
        mComments.clear();
        notifyItemRangeRemoved(0, oldSize);
    }

    public void setCommit(Commit commit) {
        mCommit = commit;
        clear();
        mPage = 1;
        mLoader.loadCommitComments(this, mCommit.getFullRepoName(), mCommit.getSha(), mPage);
    }

    @Override
    public void listLoadComplete(List<Comment> comments) {
        mRefresher.setRefreshing(false);
        mIsLoading = false;
        if(comments.size() > 0) {
            final int oldLength = mComments.size();
            for(Comment c : comments) {
                mComments.add(Pair.create(c, null));
            }
            notifyItemRangeInserted(oldLength, mComments.size());
        } else {
            mMaxPageReached = true;
        }
    }

    @Override
    public void listLoadError(APIHandler.APIError error) {
        mRefresher.setRefreshing(false);
    }

    public void notifyBottomReached() {
        if(!mIsLoading && !mMaxPageReached) {
            mPage++;
            loadComments(false);
        }
    }

    private void loadComments(boolean resetPage) {
        mIsLoading = true;
        mRefresher.setRefreshing(true);
        if(resetPage) {
            mPage = 1;
            mMaxPageReached = false;
        }
        mLoader.loadCommitComments(this, mCommit.getFullRepoName(), mCommit.getSha(), mPage);
    }

    public void addComment(Comment comment) {
        mComments.add(Pair.create(comment, null));
        notifyItemInserted(mComments.size());
    }

    public void removeComment(int commentId) {
        for(int i = 0; i < mComments.size(); i++) {
            if(mComments.get(i).first.getId() == commentId) {
                mComments.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    public void updateComment(Comment comment) {
        for(int i = 0; i < mComments.size(); i++) {
            if(mComments.get(i).first.getId() == comment.getId()) {
                mComments.set(i, Pair.create(comment, null));
                notifyItemChanged(i);
                break;
            }
        }
    }

    @Override
    public CommentHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new CommentHolder(LayoutInflater.from(parent.getContext())
                                               .inflate(R.layout.viewholder_comment, parent,
                                                       false
                                               ));

    }

    @Override
    public void onBindViewHolder(CommentHolder holder, int position) {
        final int pos = holder.getAdapterPosition();
        final Comment comment = mComments.get(pos).first;
        if(mComments.get(pos).second == null) {
            holder.mAvatar.setImageUrl(comment.getUser().getAvatarUrl());
            final StringBuilder builder = new StringBuilder();
            builder.append(String.format(
                    holder.itemView.getResources().getString(R.string.text_comment_by),
                    String.format(
                            holder.itemView.getResources().getString(R.string.text_href),
                            comment.getUser().getHtmlUrl(),
                            comment.getUser().getLogin()
                    ),
                    DateUtils.getRelativeTimeSpanString(comment.getCreatedAt())
            ));
            if(comment.getUpdatedAt() != comment.getCreatedAt()) {
                builder.append(" • ");
                builder.append(holder.itemView.getResources()
                                                     .getString(R.string.text_comment_edited));
            }
            holder.mCommenter.setMarkdown(builder.toString());
            builder.setLength(0);
            builder.append(Markdown.formatMD(comment.getBody(), mCommit.getFullRepoName()));
            if(comment.hasReaction()) {
                builder.append("\n");
                builder.append(Formatter.reactions(comment.getReaction()));
            }

            holder.mBody.setMarkdown(
                    builder.toString(),
                    new HttpImageGetter(holder.mBody),
                    text -> mComments.set(pos, Pair.create(comment, text))
            );
        } else {
            holder.mAvatar.setImageUrl(comment.getUser().getAvatarUrl());
            holder.mBody.setText(mComments.get(pos).second);
        }
        IntentHandler.addOnClickHandler(mParent.getActivity(), holder.mBody);
        IntentHandler.addOnClickHandler(mParent.getActivity(), holder.mAvatar,
                comment.getUser().getLogin()
        );
        holder.mMenu.setOnClickListener((v) -> displayMenu(v, holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return mComments.size();
    }

    private void displayMenu(View view, int pos) {
        mParent.displayCommentMenu(view, mComments.get(pos).first);
    }

    static class CommentHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.event_comment_avatar) NetworkImageView mAvatar;
        @BindView(R.id.comment_commenter) MarkdownTextView mCommenter;
        @BindView(R.id.comment_text) MarkdownTextView mBody;
        @BindView(R.id.comment_menu_button) ImageButton mMenu;

        CommentHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

    }
}
