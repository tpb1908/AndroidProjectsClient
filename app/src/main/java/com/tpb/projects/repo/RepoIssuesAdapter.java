package com.tpb.projects.repo;

import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.tpb.github.data.APIHandler;
import com.tpb.github.data.Loader;
import com.tpb.github.data.models.Issue;
import com.tpb.github.data.models.Label;
import com.tpb.github.data.models.Repository;
import com.tpb.github.data.models.State;
import com.tpb.mdtext.Markdown;
import com.tpb.mdtext.views.MarkdownTextView;
import com.tpb.projects.R;
import com.tpb.projects.common.NetworkImageView;
import com.tpb.projects.flow.IntentHandler;
import com.tpb.projects.markdown.Formatter;
import com.tpb.projects.repo.fragments.RepoIssuesFragment;
import com.tpb.projects.util.Util;
import com.tpb.projects.util.search.FuzzyStringSearcher;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 25/03/17.
 */

public class RepoIssuesAdapter extends RecyclerView.Adapter<RepoIssuesAdapter.IssueHolder> implements Loader.ListLoader<Issue> {

    private final RepoIssuesFragment mParent;
    private final SwipeRefreshLayout mRefresher;
    private final ArrayList<Pair<Issue, SpannableString>> mIssues = new ArrayList<>();
    private FuzzyStringSearcher mSearcher = new FuzzyStringSearcher();
    private boolean mIsSearching = false;
    private List<Integer> mSearchFilter = new ArrayList<>();

    private Loader mLoader;
    private Repository mRepo;
    private int mPage = 1;
    private boolean mIsLoading = false;
    private boolean mMaxPageReached = false;

    private State mFilter = State.OPEN;
    private String mAssigneeFilter;
    private final ArrayList<String> mLabelsFilter = new ArrayList<>();

    public RepoIssuesAdapter(RepoIssuesFragment parent, SwipeRefreshLayout refresher) {
        mParent = parent;
        mLoader = Loader.getLoader(mParent.getContext());
        mRefresher = refresher;
        mRefresher.setOnRefreshListener(() -> {
            final int oldSize = mIssues.size();
            mIssues.clear();
            mIsSearching = false;
            notifyItemRangeRemoved(0, oldSize);
            loadIssues(true);
        });
    }

    public void setRepo(Repository repo) {
        mRepo = repo;
        mIssues.clear();
        loadIssues(true);
    }

    public void search(String query) {
        if(mIsLoading) return;
        mIsSearching = true;
        final List<String> issues = new ArrayList<>();
        final StringBuilder builder = new StringBuilder();
        for(Pair<Issue, SpannableString> p : mIssues) {
            builder.append(p.first.getNumber());
            builder.append(" ");
            builder.append(p.first.getTitle());
            if(p.first.getOpenedBy() != null) builder.append(p.first.getOpenedBy().getLogin());
            if(p.first.getLabels() != null) {
                for(Label l : p.first.getLabels()) builder.append(l.getName());
            }
            builder.append(" ");
            builder.append(p.first.getBody());
            issues.add(builder.toString());
            builder.setLength(0);
        }
        mSearcher.setItems(issues);
        mSearchFilter = mSearcher.search(query);
        notifyDataSetChanged();
    }

    public void closeSearch() {
        mIsSearching = false;
        mSearchFilter.clear();
    }

    public void applyFilter(State state, String assignee, ArrayList<String> labels) {
        mIsSearching = false;
        mSearchFilter.clear();
        mFilter = state;
        mAssigneeFilter = assignee;
        mLabelsFilter.clear();
        mLabelsFilter.addAll(labels);
        final int oldSize = mIssues.size();
        mIssues.clear();
        notifyItemRangeRemoved(0, oldSize);
        loadIssues(true);
    }

    @Override
    public void listLoadComplete(List<Issue> issues) {
        mRefresher.setRefreshing(false);
        mIsLoading = false;
        if(issues.size() > 0) {
            final int oldLength = mIssues.size();
            for(Issue i : issues) {
                mIssues.add(Pair.create(i, null));
            }
            notifyItemRangeInserted(oldLength, mIssues.size());
        } else {
            mMaxPageReached = true;
        }
    }

    @Override
    public void listLoadError(APIHandler.APIError error) {
        mRefresher.setRefreshing(false);
        loadIssues(false);
    }

    public void notifyBottomReached() {
        if(!mIsLoading && !mMaxPageReached) {
            mPage++;
            loadIssues(false);
        }
    }

    private void loadIssues(boolean resetPage) {
        mIsLoading = true;
        mRefresher.setRefreshing(true);
        if(resetPage) {
            mPage = 1;
            mMaxPageReached = false;
        }
        mLoader.loadIssues(this, mRepo.getFullName(), mFilter, mAssigneeFilter, mLabelsFilter,
                mPage
        );
    }

    public void addIssue(Issue issue) {
        mIssues.add(0, Pair.create(issue, null));
        notifyItemInserted(0);
    }

    public void updateIssue(Issue issue) {
        int index = Util.indexOf(mIssues, issue);
        if(index != -1) {
            mIssues.set(index, Pair.create(issue, null));
            notifyItemChanged(index);
        }
    }

    @Override
    public IssueHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new IssueHolder(LayoutInflater.from(parent.getContext())
                                             .inflate(R.layout.viewholder_issue, parent, false));
    }

    @Override
    public void onBindViewHolder(IssueHolder holder, int position) {
        final int pos = mIsSearching ? mSearchFilter.get(position) : holder.getAdapterPosition();
        final Issue issue = mIssues.get(pos).first;
        holder.mTitle.setMarkdown(Formatter.bold(issue.getTitle()));
        holder.mIssueIcon.setImageResource(
                issue.isClosed() ? R.drawable.ic_state_closed : R.drawable.ic_state_open);
        holder.mUserAvatar.setImageUrl(issue.getOpenedBy().getAvatarUrl());
        IntentHandler.addOnClickHandler(mParent.getActivity(), holder.mUserAvatar,
                issue.getOpenedBy().getLogin()
        );
        IntentHandler
                .addOnClickHandler(mParent.getActivity(), holder.mContent, holder.mUserAvatar, null,
                        issue
                );
        if(mIssues.get(pos).second == null) {
            holder.mContent.setMarkdown(Markdown.formatMD(
                    Formatter.buildCombinedIssueSpan(holder.itemView.getContext(), issue).toString(),
                    issue.getRepoFullName()
                    ),
                    null,
                    text -> mIssues.set(pos, Pair.create(issue, text))
            );

        } else {
            holder.mContent.setText(mIssues.get(pos).second);
        }
        IntentHandler.addOnClickHandler(mParent.getActivity(), holder.mContent, issue);
        IntentHandler.addOnClickHandler(mParent.getActivity(), holder.mTitle, issue);
        IntentHandler.addOnClickHandler(mParent.getActivity(), holder.itemView, issue);
        holder.mMenuButton.setOnClickListener((v) -> mParent.openMenu(v, issue));
    }

    @Override
    public int getItemCount() {
        return mIsSearching ? mSearchFilter.size() : mIssues.size();
    }

    static class IssueHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.issue_title) MarkdownTextView mTitle;
        @BindView(R.id.issue_content_markdown) MarkdownTextView mContent;
        @BindView(R.id.issue_menu_button) ImageButton mMenuButton;
        @BindView(R.id.issue_state_drawable) ImageView mIssueIcon;
        @BindView(R.id.issue_user_avatar) NetworkImageView mUserAvatar;

        IssueHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
