package com.tpb.projects.commits.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.tpb.animatingrecyclerview.AnimatingRecyclerView;
import com.tpb.github.data.APIHandler;
import com.tpb.github.data.Loader;
import com.tpb.github.data.models.Commit;
import com.tpb.projects.R;
import com.tpb.projects.commits.CommitActivity;
import com.tpb.projects.commits.CommitDiffAdapter;
import com.tpb.projects.flow.IntentHandler;
import com.tpb.mdtext.Markdown;
import com.tpb.projects.markdown.Spanner;
import com.tpb.projects.common.FixedLinearLayoutManger;
import com.tpb.projects.common.NetworkImageView;
import com.tpb.projects.util.Util;

import com.tpb.mdtext.views.MarkdownTextView;

import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by theo on 30/03/17.
 */

public class CommitInfoFragment extends CommitFragment {

    private Unbinder unbinder;

    @BindView(R.id.commit_header_card) View mHeader;
    @BindView(R.id.commit_title) MarkdownTextView mTitle;
    @BindView(R.id.commit_user_avatar) NetworkImageView mAvatar;
    @BindView(R.id.commit_info) MarkdownTextView mInfo;
    @BindView(R.id.commit_info_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.commit_info_scrollview) NestedScrollView mScrollView;
    @BindView(R.id.commit_diff_recycler) AnimatingRecyclerView mRecyclerView;

    private CommitDiffAdapter mAdapter;

    public static CommitInfoFragment getInstance(CommitActivity parent) {
        final CommitInfoFragment cif = new CommitInfoFragment();
        cif.mParent = parent;
        return cif;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_commit_info, container, false);
        unbinder = ButterKnife.bind(this, view);
        mRefresher.setRefreshing(true);
        mAdapter = new CommitDiffAdapter();
        mRecyclerView.setLayoutManager(new FixedLinearLayoutManger(getContext()));
        mRecyclerView.setAdapter(mAdapter);
        mRefresher.setOnRefreshListener(() -> {
            mAdapter.clear();
            new Loader(getContext()).loadCommit(new Loader.ItemLoader<Commit>() {
                @Override
                public void loadComplete(Commit commit) {
                    commitLoaded(commit);
                }

                @Override
                public void loadError(APIHandler.APIError error) {
                    mRefresher.setRefreshing(false);
                }
            }, mCommit.getFullRepoName(), mCommit.getSha());
        });
        checkSharedElementEntry();
        mAreViewsValid = true;
        if(mCommit != null) commitLoaded(mCommit);
        return view;
    }

    @Override
    public void commitLoaded(Commit commit) {
        mCommit = commit;
        if(!mAreViewsValid) return;
        mTitle.setMarkdown(Spanner.bold(Markdown.escape(mCommit.getMessage())));
        final String user;
        if(mCommit.getCommitter() != null) {
            mAvatar.setImageUrl(mCommit.getCommitter().getAvatarUrl());
            IntentHandler
                    .addOnClickHandler(getActivity(), mAvatar, mCommit.getCommitter().getLogin());
            user = String.format(getString(R.string.text_md_link),
                    mCommit.getCommitter().getLogin(),
                    mCommit.getCommitter().getHtmlUrl()
            );
        } else {
            user = mCommit.getCommitterName();
            IntentHandler.addOnClickHandler(getActivity(), mAvatar, user);
        }

        if(mCommit.getFiles() != null) {
            String builder =
                    "<br>" +
                            getResources()
                                    .getQuantityString(R.plurals.text_commit_additions,
                                            mCommit.getAdditions(),
                                            mCommit.getAdditions()
                                    ) +
                            "<br>" +
                            getResources().getQuantityString(R.plurals.text_commit_deletions,
                                    mCommit.getDeletions(), mCommit.getDeletions()
                            ) +
                            "<br><br>" +
                            String.format(
                                    getString(R.string.text_committed_by),
                                    user,
                                    Util.formatDateLocally(getContext(),
                                            new Date(mCommit.getCreatedAt())
                                    )
                            );
            mInfo.setMarkdown(Markdown.formatMD(builder, mCommit.getFullRepoName()));
            mRefresher.setRefreshing(false);
            mAdapter.setDiffs(mCommit.getFiles());
        }

    }

    private void checkSharedElementEntry() {
        final Intent i = getActivity().getIntent();
        if(i.hasExtra(getString(R.string.transition_card))) {
            mHeader.getViewTreeObserver()
                   .addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                       @Override
                       public boolean onPreDraw() {
                           mHeader.getViewTreeObserver().removeOnPreDrawListener(this);
                           if(i.hasExtra(getString(R.string.intent_drawable))) {
                               mAvatar.setImageBitmap(
                                       i.getParcelableExtra(getString(R.string.intent_drawable)));
                           }
                           getActivity().startPostponedEnterTransition();
                           return true;
                       }
                   });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
