package com.tpb.projects.commits.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tpb.animatingrecyclerview.AnimatingRecyclerView;
import com.tpb.projects.R;
import com.tpb.projects.commits.CommitActivity;
import com.tpb.projects.data.models.Commit;
import com.tpb.projects.markdown.Markdown;

import org.sufficientlysecure.htmltext.htmltextview.HtmlTextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by theo on 30/03/17.
 */

public class CommitInfoFragment extends CommitFragment {

    private Unbinder unbinder;

    @BindView(R.id.commit_title) HtmlTextView mTitle;
    @BindView(R.id.commit_info) HtmlTextView mInfo;
    @BindView(R.id.commit_info_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.commit_info_scrollview) NestedScrollView mScrollView;
    @BindView(R.id.commit_diff_recycler) AnimatingRecyclerView mRecyclerView;

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
        mAreViewsValid = true;
        if(mCommit != null) commitLoaded(mCommit);
        return view;
    }

    @Override
    public void commitLoaded(Commit commit) {
        mCommit = commit;
        if(!mAreViewsValid) return;
        mTitle.setHtml(Markdown.escape(mCommit.getMessage()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
