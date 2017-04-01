package com.tpb.projects.repo;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.androidnetworking.widget.ANImageView;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Label;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.data.models.State;
import com.tpb.projects.flow.IntentHandler;
import com.tpb.projects.issues.IssueActivity;
import com.tpb.projects.markdown.Markdown;
import com.tpb.projects.markdown.Spanner;
import com.tpb.projects.repo.fragments.RepoIssuesFragment;
import com.tpb.projects.util.UI;
import com.tpb.projects.util.Util;
import com.tpb.projects.util.search.FuzzyStringSearcher;

import org.sufficientlysecure.htmltext.htmltextview.HtmlTextView;

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
    private ArrayList<Integer> mSearchFilter = new ArrayList<>();

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
        mLoader = new Loader(mParent.getContext());
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
        loadIssues(true);
    }

    public void search(String query) {
        if(mIsLoading) return;
        mIsSearching = true;
        final ArrayList<String> issues = new ArrayList<>();
        String s;
        for(Pair<Issue, SpannableString> p : mIssues) {
            s = "#" + p.first.getNumber();
            if(p.first.getLabels() != null) {
                for(Label l : p.first.getLabels()) s += "\n" + l.getName();
            }
            s += p.first.getTitle() + "\n" + p.first.getBody();
            issues.add(s);
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
            if(mPage == 1) mIssues.clear();
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
        mLoader.loadIssues(this, mRepo.getFullName(), mFilter, mAssigneeFilter, mLabelsFilter, mPage);
    }


    public void addIssue(Issue issue) {
        mIssues.add(0, Pair.create(issue, null));
        notifyItemInserted(0);
    }

    public void updateIssue(Issue issue) {
        int index = Util.indexInPair(mIssues, issue);
        if(index != -1) {
            mIssues.set(index, Pair.create(issue, null));
            notifyItemChanged(index);
        }
    }
    @Override
    public IssueHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new IssueHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_issue, parent, false));
    }

    @Override
    public void onBindViewHolder(IssueHolder holder, int position) {
        final int pos;
        if(mIsSearching) {
            pos = mSearchFilter.get(position);
        } else {
            pos = holder.getAdapterPosition();
        }
        final Issue issue = mIssues.get(pos).first;
        holder.mTitle.setHtml(Spanner.bold(issue.getTitle()));
        holder.mIssueIcon.setImageResource(issue.isClosed() ? R.drawable.ic_state_closed : R.drawable.ic_state_open);
        holder.mUserAvatar.setImageUrl(issue.getOpenedBy().getAvatarUrl());
        IntentHandler.addOnClickHandler(mParent.getActivity(), holder.mUserAvatar, issue.getOpenedBy().getLogin());
        IntentHandler.addOnClickHandler(mParent.getActivity(), holder.mContent, holder.mUserAvatar, null, issue);
        if(mIssues.get(pos).second == null) {
            holder.mContent.setHtml(Markdown.parseMD(
                    Spanner.buildCombinedIssueSpan(holder.itemView.getContext(), issue).toString(), issue.getRepoFullName()),
                    null,
                    text -> mIssues.set(pos, Pair.create(issue, text))
            );

        } else {
            holder.mContent.setText(mIssues.get(pos).second);
        }
    }

    @Override
    public int getItemCount() {
        return mIsSearching ? mSearchFilter.size() : mIssues.size();
    }

    private void openIssue(IssueHolder holder, int pos) {
        final Intent i = new Intent(mParent.getContext(), IssueActivity.class);
        i.putExtra(mParent.getString(R.string.transition_card), "");
        i.putExtra(mParent.getString(R.string.parcel_issue), mIssues.get(pos).first);
        i.putExtra(mParent.getString(R.string.intent_drawable), ((BitmapDrawable) holder.mUserAvatar.getDrawable()).getBitmap());
        //We have to add the nav bar as ViewOverlay is above it
        mParent.startActivity(i, ActivityOptionsCompat.makeSceneTransitionAnimation(
                mParent.getActivity(),
                Pair.create(holder.itemView, mParent.getString(R.string.transition_card)),
                UI.getSafeNavigationBarTransitionPair(mParent.getActivity())).toBundle()
        );
    }

    class IssueHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.issue_title) HtmlTextView mTitle;
        @BindView(R.id.issue_content_markdown) HtmlTextView mContent;
        @BindView(R.id.issue_menu_button) ImageButton mMenuButton;
        @BindView(R.id.issue_state_drawable) ImageView mIssueIcon;
        @BindView(R.id.issue_user_avatar) ANImageView mUserAvatar;

        IssueHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            mMenuButton.setOnClickListener((v) -> mParent.openMenu(v, mIssues.get(getAdapterPosition()).first));
            mContent.setConsumeNonUrlClicks(false);
            mTitle.setConsumeNonUrlClicks(false);
            view.setOnClickListener((v) -> openIssue(IssueHolder.this, getAdapterPosition()));
        }
    }
}
