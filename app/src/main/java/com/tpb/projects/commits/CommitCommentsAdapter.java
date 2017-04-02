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
import com.tpb.projects.R;
import com.tpb.projects.commits.fragments.CommitCommentsFragment;
import com.tpb.projects.flow.IntentHandler;
import com.tpb.projects.markdown.Markdown;
import com.tpb.projects.util.NetworkImageView;

import org.sufficientlysecure.htmltext.dialogs.CodeDialog;
import org.sufficientlysecure.htmltext.dialogs.ImageDialog;
import org.sufficientlysecure.htmltext.htmltextview.HtmlTextView;
import org.sufficientlysecure.htmltext.imagegetter.HtmlHttpImageGetter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 01/04/17.
 */

public class CommitCommentsAdapter extends RecyclerView.Adapter<CommitCommentsAdapter.CommentHolder> implements Loader.ListLoader<Comment> {
    private static final String TAG = CommitCommentsAdapter.class.getSimpleName();

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
        mLoader = new Loader(mParent.getContext());
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
            if(mPage == 1) mComments.clear();
            int oldLength = mComments.size();
            for(Comment c : comments) {
                mComments.add(new Pair<>(c, null));
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
        mComments.add(new Pair<>(comment, null));
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
            mComments.set(index, new Pair<>(comment, null));
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
        bindComment(holder);
    }

    private void bindComment(CommentHolder commentHolder) {
        final int pos = commentHolder.getAdapterPosition();
        final Comment comment = mComments.get(pos).first;
        if(mComments.get(pos).second == null) {
            commentHolder.mAvatar.setImageUrl(comment.getUser().getAvatarUrl());
            final StringBuilder builder = new StringBuilder();
            builder.append(String.format(
                    commentHolder.itemView.getResources().getString(R.string.text_comment_by),
                    String.format(
                            commentHolder.itemView.getResources().getString(R.string.text_href),
                            comment.getUser().getHtmlUrl(),
                            comment.getUser().getLogin()
                    ),
                    DateUtils.getRelativeTimeSpanString(comment.getCreatedAt())
            ));
            if(comment.getUpdatedAt() != comment.getCreatedAt()) {
                builder.append(" • ");
                builder.append(commentHolder.itemView.getResources()
                                                     .getString(R.string.text_comment_edited));
            }
            builder.append("<br><br>");
            builder.append(Markdown.formatMD(comment.getBody(), mCommit.getFullRepoName()));
            commentHolder.mText.setHtml(
                    Markdown.parseMD(builder.toString()),
                    new HtmlHttpImageGetter(commentHolder.mText, commentHolder.mText),
                    text -> mComments.set(pos, new Pair<>(comment, text))
            );
        } else {
            commentHolder.mAvatar.setImageUrl(comment.getUser().getAvatarUrl());
            commentHolder.mText.setText(mComments.get(pos).second);
        }
        IntentHandler.addOnClickHandler(mParent.getActivity(), commentHolder.mText);
        IntentHandler.addOnClickHandler(mParent.getActivity(), commentHolder.mAvatar,
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
        @BindView(R.id.comment_text) HtmlTextView mText;
        @BindView(R.id.comment_menu_button) ImageButton mMenu;

        CommentHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            mText.setImageHandler(new ImageDialog(mText.getContext()));
            mText.setCodeClickHandler(new CodeDialog(mText.getContext()));
            mMenu.setOnClickListener((v) -> displayMenu(v, getAdapterPosition()));
            // view.setOnClickListener((v) -> displayInFullScreen(getAdapterPosition()));
        }

    }
}
