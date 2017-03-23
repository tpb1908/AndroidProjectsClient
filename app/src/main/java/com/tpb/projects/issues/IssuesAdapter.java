package com.tpb.projects.issues;

import android.content.Intent;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.androidnetworking.widget.ANImageView;
import com.tpb.projects.R;
import com.tpb.projects.markdown.Spanner;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.flow.IntentHandler;
import com.tpb.projects.markdown.Markdown;
import com.tpb.projects.util.UI;

import org.sufficientlysecure.htmltext.htmltextview.HtmlTextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 27/01/17.
 */

class IssuesAdapter extends RecyclerView.Adapter<IssuesAdapter.IssueHolder> {
    private static final String TAG = IssuesAdapter.class.getSimpleName();

    private final IssuesActivity mParent;
    private final ArrayList<Issue> mIssues = new ArrayList<>();
    private final ArrayList<SpannableString> mParseCache = new ArrayList<>();

    IssuesAdapter(IssuesActivity parent) {
        mParent = parent;
    }

    void loadIssues(Issue[] issues) {
        mIssues.clear();
        mParseCache.clear();
        for(Issue i : issues) {
            mIssues.add(i);
            mParseCache.add(null);
        }
        notifyItemRangeInserted(0, issues.length);
    }

    void addIssue(Issue issue) {
        mIssues.add(0, issue);
        mParseCache.add(0, null);
        notifyItemInserted(0);
    }

    void addIssues(Issue[] newIssues) {
        final int oldSize = mIssues.size();
        for(Issue i : newIssues) {
            mIssues.add(i);
            mParseCache.add(null);
        }
        notifyItemRangeInserted(oldSize, mIssues.size());
    }

    ArrayList<Issue> getIssues() {
        return mIssues;
    }

    void clear() {
        mIssues.clear();
        notifyDataSetChanged();
    }

    void updateIssue(Issue issue) {
        int index = mIssues.indexOf(issue);
        if(index != -1) {
            mIssues.set(index, issue);
            mParseCache.set(index, null);
            notifyItemChanged(index);
        }
    }

    int indexOf(Issue issue) {
        return mIssues.indexOf(issue);
    }

    @Override
    public IssueHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new IssueHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_issue, parent, false));
    }

    //TODO Move some of this binding to methods for use throughout the app
    @Override
    public void onBindViewHolder(IssueHolder holder, int position) {
        final int pos = holder.getAdapterPosition();
        final Issue issue = mIssues.get(pos);
        holder.mTitle.setHtml(Spanner.bold(issue.getTitle()));
        holder.mIssueIcon.setImageResource(issue.isClosed() ? R.drawable.ic_state_closed : R.drawable.ic_state_open);
        holder.mUserAvatar.setImageUrl(issue.getOpenedBy().getAvatarUrl());
        IntentHandler.addOnClickHandler(mParent, holder.mUserAvatar, issue.getOpenedBy().getLogin());
        IntentHandler.addOnClickHandler(mParent, holder.mContent, holder.mUserAvatar, null, issue);
        if(mParseCache.get(pos) == null) {
            holder.mContent.setHtml(Markdown.parseMD(
                    Spanner.buildCombinedIssueSpan(holder.itemView.getContext(), issue).toString(), issue.getRepoPath()),
                    null,
                    text -> mParseCache.set(pos, text)
            );

        } else {
            holder.mContent.setText(mParseCache.get(pos));
        }
    }

    @Override
    public int getItemCount() {
        return mIssues.size();
    }

    private void openIssue(View view, int pos) {
        final Intent i = new Intent(mParent, IssueActivity.class);
        i.putExtra(mParent.getString(R.string.transition_card), "");
        i.putExtra(mParent.getString(R.string.parcel_issue), mIssues.get(pos));
        //We have to add the nav bar as ViewOverlay is above it
        mParent.startActivity(i, ActivityOptionsCompat.makeSceneTransitionAnimation(
                mParent,
                Pair.create(view, mParent.getString(R.string.transition_card)),
                UI.getSafeNavigationBarTransitionPair(mParent)).toBundle()
        );
    }

    private void openMenu(View view, int pos) {
        mParent.openMenu(view, mIssues.get(pos));
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
            mMenuButton.setOnClickListener((v) -> openMenu(v, getAdapterPosition()));
            mContent.setConsumeNonUrlClicks(false);
            mTitle.setConsumeNonUrlClicks(false);
            view.setOnClickListener((v) -> openIssue(v, getAdapterPosition()));
        }


    }
}
