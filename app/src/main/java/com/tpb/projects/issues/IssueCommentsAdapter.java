package com.tpb.projects.issues;

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
import com.tpb.github.data.models.Issue;
import com.tpb.mdtext.Markdown;
import com.tpb.mdtext.imagegetter.HttpImageGetter;
import com.tpb.mdtext.views.MarkdownTextView;
import com.tpb.projects.R;
import com.tpb.projects.common.NetworkImageView;
import com.tpb.projects.flow.IntentHandler;
import com.tpb.projects.issues.fragments.IssueCommentsFragment;
import com.tpb.projects.markdown.Formatter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 14/03/17.
 */

public class IssueCommentsAdapter extends RecyclerView.Adapter<IssueCommentsAdapter.CommentHolder> implements Loader.ListLoader<Comment> {

    private final ArrayList<Pair<Comment, SpannableString>> mComments = new ArrayList<>();
    private Issue mIssue;
    private final IssueCommentsFragment mParent;

    private int mPage = 1;
    private boolean mIsLoading = false;
    private boolean mMaxPageReached = false;

    private SwipeRefreshLayout mRefresher;
    private Loader mLoader;

    public IssueCommentsAdapter(IssueCommentsFragment parent, SwipeRefreshLayout refresher) {
        mParent = parent;
        mLoader = Loader.getLoader(parent.getContext());
        mRefresher = refresher;
        mRefresher.setRefreshing(true);
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

    public void setIssue(Issue issue) {
        mIssue = issue;
        clear();
        mPage = 1;
        mLoader.loadIssueComments(this, mIssue.getRepoFullName(), mIssue.getNumber(), mPage);
    }

    @Override
    public void listLoadComplete(List<Comment> data) {
        mRefresher.setRefreshing(false);
        mIsLoading = false;
        if(data.size() > 0) {
            int oldLength = mComments.size();
            for(Comment c : data) {
                mComments.add(Pair.create(c, null));
            }
            notifyItemRangeInserted(oldLength, mComments.size());
        } else {
            mMaxPageReached = true;
        }
    }

    @Override
    public void listLoadError(APIHandler.APIError error) {

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
        mLoader.loadIssueComments(this, mIssue.getRepoFullName(), mIssue.getNumber(), mPage);
    }

    public void addComment(Comment comment) {
        mComments.add(Pair.create(comment, null));
        notifyItemInserted(mComments.size());
    }

    public void removeComment(int commentId) {
        int index = -1;
        for(int i = 0; i < mComments.size(); i++) {
            if(mComments.get(i).first.getId() == commentId) {
                index = i;
                break;
            }
        }
        if(index != -1) {
            mComments.remove(index);
            notifyItemRemoved(index);
        }
    }

    public void updateComment(Comment comment) {
        int index = -1;
        for(int i = 0; i < mComments.size(); i++) {
            if(mComments.get(i).first.getId() == comment.getId()) {
                index = i;
                break;
            }
        }
        if(index != -1) {
            mComments.set(index, Pair.create(comment, null));
            notifyItemChanged(index);
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
            builder.append("<br><br>");
            builder.append(Markdown.formatMD(comment.getBody(), mIssue.getRepoFullName()));
            if(comment.hasReaction()) {
                builder.append("\n");
                builder.append(Formatter.reactions(comment.getReaction()));
            }

            holder.mText.setMarkdown(
                    builder.toString(),
                    new HttpImageGetter(holder.mText),
                    text -> mComments.set(pos, Pair.create(comment, text))
            );
        } else {
            holder.mAvatar.setImageUrl(comment.getUser().getAvatarUrl());
            holder.mText.setText(mComments.get(pos).second);
        }
        IntentHandler.addOnClickHandler(mParent.getActivity(), holder.mText);
        IntentHandler.addOnClickHandler(mParent.getActivity(), holder.mAvatar,
                comment.getUser().getLogin()
        );
    }

    @Override
    public int getItemCount() {
        return mComments.size();
    }

    private void displayMenu(View view, int pos) {
        mParent.displayCommentMenu(view, mComments.get(pos).first);
    }

    class CommentHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.event_comment_avatar) NetworkImageView mAvatar;
        @BindView(R.id.comment_text) MarkdownTextView mText;
        @BindView(R.id.comment_menu_button) ImageButton mMenu;

        CommentHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            mMenu.setOnClickListener((v) -> displayMenu(v, getAdapterPosition()));
            // view.setOnClickListener((v) -> displayInFullScreen(getAdapterPosition()));
        }

    }
}
