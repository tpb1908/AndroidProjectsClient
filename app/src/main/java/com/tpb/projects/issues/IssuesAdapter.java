package com.tpb.projects.issues;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.androidnetworking.widget.ANImageView;
import com.tpb.projects.R;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Label;
import com.tpb.projects.data.models.Milestone;
import com.tpb.projects.data.models.User;
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
        holder.mIssueIcon.setImageResource(issue.isClosed() ? R.drawable.ic_state_closed : R.drawable.ic_state_open);
        holder.mUserAvatar.setImageUrl(issue.getOpenedBy().getAvatarUrl());
        IntentHandler.addGitHubIntentHandler(mParent, holder.mUserAvatar, issue.getOpenedBy().getLogin());
        IntentHandler.addGitHubIntentHandler(mParent, holder.mContent, holder.mUserAvatar, issue);

        if(mParseCache.get(pos) == null) {
            final Context context = holder.itemView.getContext();
            final StringBuilder builder = new StringBuilder();
            builder.append("<b>");
            builder.append(Markdown.escape(issue.getTitle()));
            builder.append("</b><br><br>");

            builder.append(String.format(context.getString(R.string.text_issue_opened_by),
                    String.format(context.getString(R.string.text_md_link),
                            "#" + Integer.toString(issue.getNumber()),
                            "https://github.com/" + issue.getRepoPath() + "/issues/" + Integer.toString(issue.getNumber())
                    ),
                    String.format(context.getString(R.string.text_md_link),
                            issue.getOpenedBy().getLogin(),
                            issue.getOpenedBy().getHtmlUrl()
                    ),
                    DateUtils.getRelativeTimeSpanString(issue.getCreatedAt()))
            );

            if(issue.getAssignees() != null) {
                builder.append("<br>");
                builder.append(context.getString(R.string.text_assigned_to));
                builder.append(' ');
                for(User u : issue.getAssignees()) {
                    builder.append(String.format(context.getString(R.string.text_md_link),
                            u.getLogin(),
                            u.getHtmlUrl()));
                    builder.append(' ');
                }
            }

            if(issue.isClosed() && issue.getClosedBy() != null) {
                builder.append("<br>");
                builder.append(String.format(context.getString(R.string.text_closed_by_link),
                        issue.getClosedBy().getLogin(),
                        issue.getClosedBy().getHtmlUrl(),
                        DateUtils.getRelativeTimeSpanString(issue.getClosedAt())));
            }

            if(issue.getLabels() != null && issue.getLabels().length > 0) {
                builder.append("<br>");
                Label.appendLabels(builder, issue.getLabels(), "   ");
            }
            if(issue.getComments() > 0) {
                builder.append("<br>");
                builder.append(context.getResources().getQuantityString(R.plurals.text_issue_comment_count, issue.getComments(), issue.getComments()));
            }
            if(issue.getMilestone() != null) {
                final Milestone milestone = issue.getMilestone();
                builder.append("<br>");
                if(milestone.getClosedAt() == 0) {


                    if(milestone.getDueOn() > 0) {
                        final StringBuilder dueStringBuilder = new StringBuilder();
                        if(System.currentTimeMillis() < milestone.getDueOn() ||
                                (milestone.getClosedAt() != 0 && milestone.getClosedAt() < milestone.getDueOn())) {
                            dueStringBuilder.append(
                                    String.format(
                                            context.getString(R.string.text_milestone_due_on),
                                            DateUtils.getRelativeTimeSpanString(milestone.getDueOn())
                                    )
                            );
                        } else {
                            dueStringBuilder.append("<font color=\"");
                            dueStringBuilder.append(String.format("#%06X", (0xFFFFFF & Color.RED)));
                            dueStringBuilder.append("\">");
                            dueStringBuilder.append(
                                    String.format(
                                            context.getString(R.string.text_milestone_due_on),
                                            DateUtils.getRelativeTimeSpanString(milestone.getDueOn())
                                    )
                            );
                            dueStringBuilder.append("</font>");
                        }
                        builder.append(
                                String.format(
                                        context.getString(R.string.text_milestone_short),
                                        String.format(
                                                context.getString(R.string.text_href),
                                                milestone.getHtmlUrl(),
                                                milestone.getTitle()),
                                        dueStringBuilder.toString()
                                )
                        );
                    } else {
                        builder.append(
                                String.format(
                                        context.getString(R.string.text_milestone_short),
                                        String.format(
                                                context.getString(R.string.text_href),
                                                milestone.getHtmlUrl(),
                                                milestone.getTitle()),
                                        String.format(
                                                context.getString(R.string.text_milestone_opened),
                                                DateUtils.getRelativeTimeSpanString(milestone.getCreatedAt())
                                        )
                                )
                        );
                    }


                } else {
                    builder.append(
                            String.format(
                                    context.getString(R.string.text_milestone_short),
                                    String.format(
                                            context.getString(R.string.text_href),
                                            milestone.getHtmlUrl(),
                                            milestone.getTitle()),
                                    String.format(
                                            context.getString(R.string.text_milestone_closed_at),
                                            DateUtils.getRelativeTimeSpanString(milestone.getClosedAt())
                                    )
                            )
                    );
                }
            }
            holder.mContent.setHtml(Markdown.parseMD(builder.toString()), null, text -> mParseCache.set(pos, text));

        } else {
            // Log.i(TAG, "onBindViewHolder: Binding pos " + pos + " with\n" + mIssues.get(pos).second);
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

    public class IssueHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.issue_content_markdown) HtmlTextView mContent;
        @BindView(R.id.issue_menu_button) ImageButton mMenuButton;
        @BindView(R.id.issue_drawable) ImageView mIssueIcon;
        @BindView(R.id.issue_user_avatar) ANImageView mUserAvatar;

        IssueHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            mContent.setShowUnderLines(false);
            mMenuButton.setOnClickListener((v) -> openMenu(v, getAdapterPosition()));
            view.setOnClickListener((v) -> openIssue(v, getAdapterPosition()));
        }


    }
}
