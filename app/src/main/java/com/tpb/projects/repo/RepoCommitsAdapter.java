package com.tpb.projects.repo;

import android.content.res.Resources;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tpb.github.data.APIHandler;
import com.tpb.github.data.Loader;
import com.tpb.github.data.models.Commit;
import com.tpb.github.data.models.Repository;
import com.tpb.mdtext.Markdown;
import com.tpb.mdtext.views.MarkdownTextView;
import com.tpb.projects.R;
import com.tpb.projects.common.NetworkImageView;
import com.tpb.projects.flow.IntentHandler;
import com.tpb.projects.repo.fragments.RepoCommitsFragment;
import com.tpb.projects.util.Util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 29/03/17.
 */

public class RepoCommitsAdapter extends RecyclerView.Adapter<RepoCommitsAdapter.CommitViewHolder> implements Loader.ListLoader<Commit> {
    private static final String TAG = RepoCommitsAdapter.class.getSimpleName();

    private RepoCommitsFragment mParent;
    private Repository mRepo;
    private String mBranch;
    private ArrayList<Pair<Commit, SpannableString>> mCommits = new ArrayList<>();

    private int mPage = 1;
    private boolean mIsLoading = false;
    private boolean mMaxPageReached = false;

    private SwipeRefreshLayout mRefresher;
    private Loader mLoader;

    public RepoCommitsAdapter(RepoCommitsFragment parent, SwipeRefreshLayout refresher) {
        mParent = parent;
        mRefresher = refresher;
        mLoader = new Loader(parent.getContext());
        mRefresher.setOnRefreshListener(() -> {
            final int oldSize = mCommits.size();
            mCommits.clear();
            notifyItemRangeRemoved(0, oldSize);
            loadCommits(true);
        });
    }

    public void setRepo(Repository repo) {
        mRepo = repo;
        loadCommits(true);
    }

    public void setBranch(String branch) {
        if(!branch.equals(mBranch)) {
            if(mBranch != null) {
                mBranch = branch;
                mCommits.clear();
                notifyDataSetChanged();
                loadCommits(true);
            } else {
                mBranch = branch;
            }

        }
    }

    public void notifyBottomReached() {
        if(!mIsLoading && !mMaxPageReached) {
            mPage++;
            loadCommits(false);
        }
    }

    private void loadCommits(boolean resetPage) {
        mIsLoading = true;
        mRefresher.setRefreshing(true);
        if(resetPage) {
            mPage = 1;
            mMaxPageReached = false;
        }
        mLoader.loadCommits(this, mRepo.getFullName(), mBranch, mPage);
    }

    @Override
    public void listLoadComplete(List<Commit> commits) {
        if(!mParent.areViewsValid()) return;
        mRefresher.setRefreshing(false);
        mIsLoading = false;
        if(commits.size() > 0) {
            final int oldLength = mCommits.size();
            if(mPage == 1) {
                mParent.setLatestSHA(commits.get(0).getSha());
                mCommits.clear();
            }
            for(Commit c : commits) {
                mCommits.add(Pair.create(c, null));
            }
            notifyItemRangeInserted(oldLength, mCommits.size());
        } else {
            mMaxPageReached = true;
        }
    }

    @Override
    public void listLoadError(APIHandler.APIError error) {
        mRefresher.setRefreshing(false);
    }

    @Override
    public CommitViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new CommitViewHolder(LayoutInflater.from(parent.getContext())
                                                  .inflate(R.layout.viewholder_commit, parent,
                                                          false
                                                  ));
    }

    @Override
    public void onBindViewHolder(CommitViewHolder holder, int position) {
        final Commit c = mCommits.get(position).first;
        if(c.getCommitter() != null) {
            holder.mAvatar.setImageUrl(c.getCommitter().getAvatarUrl());
        }
        holder.mTitle.setMarkdown(Markdown.formatMD(c.getMessage(), mRepo.getFullName()));
        final String userName;
        final String userUrl;

        if(c.getCommitter() != null) {
            userName = c.getCommitter().getLogin();
            userUrl = c.getCommitter().getHtmlUrl();
        } else {
            userName = c.getCommitterName();
            userUrl = IntentHandler.getUserUrl(userName);

        }
        if(mCommits.get(position).second == null) {
            final StringBuilder builder = new StringBuilder();
            final Resources res = holder.itemView.getResources();

            builder.append(
                    String.format(
                            res.getString(R.string.text_committed_by_with_has),
                            String.format(
                                    res.getString(R.string.text_md_link),
                                    userName,
                                    userUrl
                            ),
                            String.format(
                                    res.getString(R.string.text_md_link),
                                    com.tpb.github.data.Util.shortenSha(c.getSha()),
                                    c.getHtmlUrl()
                            ),
                            Util.formatDateLocally(holder.itemView.getContext(),
                                    new Date(c.getCreatedAt())
                            )
                    )
            );
            holder.mInfo
                    .setMarkdown(Markdown.formatMD(builder.toString(), mRepo.getFullName()), null,
                            text -> mCommits.set(position, Pair.create(c, text))
                    );
        } else {
            holder.mInfo.setText(mCommits.get(position).second);
        }
        IntentHandler.addOnClickHandler(mParent.getActivity(), holder.mAvatar, userName);
        IntentHandler.addOnClickHandler(mParent.getActivity(), holder.mTitle, c);
        IntentHandler.addOnClickHandler(mParent.getActivity(), holder.mInfo, c);
    }

    @Override
    public int getItemCount() {
        return mCommits.size();
    }

    static class CommitViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.commit_user_avatar) NetworkImageView mAvatar;
        @BindView(R.id.commit_title) MarkdownTextView mTitle;
        @BindView(R.id.commit_info) MarkdownTextView mInfo;

        CommitViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

}
