package com.tpb.projects.issues.content;

import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.androidnetworking.widget.ANImageView;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.Comment;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.flow.IntentHandler;
import com.tpb.projects.markdown.Markdown;
import com.tpb.projects.util.MultiOnRefreshListener;

import org.sufficientlysecure.htmltext.dialogs.CodeDialog;
import org.sufficientlysecure.htmltext.dialogs.ImageDialog;
import org.sufficientlysecure.htmltext.htmltextview.HtmlTextView;
import org.sufficientlysecure.htmltext.imagegetter.HtmlHttpImageGetter;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 14/03/17.
 */

public class IssueCommentsAdapter extends RecyclerView.Adapter<IssueCommentsAdapter.CommentHolder> implements Loader.GITModelsLoader<Comment> {

    private final ArrayList<Pair<Comment, SpannableString>> mComments = new ArrayList<>();
    private Issue mIssue;
    private final IssueCommentsFragment mParent;

    private int mPage = 1;
    private boolean mIsLoading = false;
    private boolean mMaxPageReached = false;

    private SwipeRefreshLayout mRefresher;
    private Loader mLoader;
    
    IssueCommentsAdapter(IssueCommentsFragment parent, SwipeRefreshLayout refresher, MultiOnRefreshListener listener) {
        mParent = parent;
        mLoader = new Loader(parent.getContext());
        mRefresher = refresher;
        mRefresher.setRefreshing(true);
        listener.addListener(() -> {
            mPage = 1;
            mMaxPageReached = false;
            notifyDataSetChanged();
            loadComments(true);
        });
    }

    void clear() {
        mComments.clear();
        notifyDataSetChanged();
    }
    
    void setIssue(Issue issue) {
        mIssue = issue;
        mComments.clear();
        //TODO Add pull request model and link to pull requests
    }

    @Override
    public void loadComplete(Comment[] data) {
        mRefresher.setRefreshing(false);
        mIsLoading = false;
        if(data.length > 0) {
            int oldLength = mComments.size();
            if(mPage == 1) mComments.clear();
            for(Comment c : data) {
                mComments.add(new Pair<>(c, null));
            }
            notifyItemRangeInserted(oldLength, mComments.size());
        } else {
            mMaxPageReached = true;
        }
    }

    @Override
    public void loadError(APIHandler.APIError error) {

    }

    public void notifyBottomReached() {
        if(!mIsLoading && !mMaxPageReached) {
            mPage++;
            loadComments(false);
        }
    }
    
    void loadComments(boolean resetPage) {
        mIsLoading = true;
        mRefresher.setRefreshing(true);
        if(resetPage) {
            mPage = 1;
            mMaxPageReached = false;
        }
        mLoader.loadComments(this, mIssue.getRepoPath(), mIssue.getNumber());
    }

    void addComment(Comment comment) {
        mComments.add(new Pair<>(comment, null));
        notifyItemInserted(mComments.size());
    }

    void removeComment(Comment comment) {
        int index = -1;
        for(int i = 0; i < mComments.size(); i++) {
            if(mComments.get(i).first.getId() == comment.getId()) {
                index = i;
                break;
            }
        }
        if(index != -1) {
            mComments.remove(index);
            notifyItemRemoved(index);
        }
    }

    void updateComment(Comment comment) {
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
    public int getItemViewType(int position) {
        return mComments.get(position).first instanceof Comment ? 1 : 0;
    }

    @Override
    public CommentHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new CommentHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_comment, parent, false));

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
            builder.append(String.format(commentHolder.itemView.getResources().getString(R.string.text_comment_by),
                    String.format(commentHolder.itemView.getResources().getString(R.string.text_href),
                            comment.getUser().getHtmlUrl(),
                            comment.getUser().getLogin()),
                    DateUtils.getRelativeTimeSpanString(comment.getCreatedAt())));
            if(comment.getUpdatedAt() != comment.getCreatedAt()) {
                builder.append(" • ");
                builder.append(commentHolder.itemView.getResources().getString(R.string.text_comment_edited));
            }
            builder.append("<br><br>");
            builder.append(Markdown.formatMD(comment.getBody(), mIssue.getRepoPath()));
            commentHolder.mText.setHtml(
                    Markdown.parseMD(builder.toString()),
                    new HtmlHttpImageGetter(commentHolder.mText, commentHolder.mText),
                    text -> mComments.set(pos, new Pair<>(comment, text)));
        } else {
            commentHolder.mAvatar.setImageUrl(comment.getUser().getAvatarUrl());
            commentHolder.mText.setText(mComments.get(pos).second);
        }
        IntentHandler.addGitHubIntentHandler(mParent.getActivity(), commentHolder.mText);
        IntentHandler.addGitHubIntentHandler(mParent.getActivity(), commentHolder.mAvatar, comment.getUser().getLogin());
    }


    @Override
    public int getItemCount() {
        return mComments.size();
    }

    private void displayMenu(View view, int pos) {
        //mParent.displayCommentMenu(view, (Comment) mComments.get(pos).first);
    }

    class CommentHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.event_comment_avatar) ANImageView mAvatar;
        @BindView(R.id.comment_text) HtmlTextView mText;
        @BindView(R.id.comment_menu_button) ImageButton mMenu;

        CommentHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            mText.setShowUnderLines(false);
            mText.setImageHandler(new ImageDialog(mText.getContext()));
            mText.setCodeClickHandler(new CodeDialog(mText.getContext()));
            mMenu.setOnClickListener((v) -> displayMenu(v, getAdapterPosition()));
           // view.setOnClickListener((v) -> displayInFullScreen(getAdapterPosition()));
        }

    }
}
