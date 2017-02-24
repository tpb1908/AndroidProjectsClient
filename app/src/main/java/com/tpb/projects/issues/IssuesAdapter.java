package com.tpb.projects.issues;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.androidnetworking.widget.ANImageView;
import com.tpb.projects.R;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Label;
import com.tpb.projects.data.models.User;
import com.tpb.projects.util.Data;
import com.tpb.projects.util.MDParser;

import org.sufficientlysecure.htmltextview.HtmlTextView;

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
    private final ArrayList<String> mParseCache = new ArrayList<>();

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
        notifyDataSetChanged();
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

    @Override
    public void onBindViewHolder(IssueHolder holder, int position) {
        final int pos = holder.getAdapterPosition();
        final Issue issue = mIssues.get(pos);
        holder.mIssueIcon.setImageResource(issue.isClosed() ? R.drawable.ic_issue_closed : R.drawable.ic_issue_open);
        holder.mUserAvatar.setImageUrl(issue.getOpenedBy().getAvatarUrl());
        holder.mUserAvatar.setOnClickListener((v) -> {
            mParent.openUser(holder.mUserAvatar, issue.getOpenedBy().getLogin());
        });
        holder.mContent.setLinkClickHandler(url -> {
            Log.i(TAG, "bindIssueCard: URL is " + url);
            if(url.startsWith("https://github.com/") && Data.instancesOf(url, "/") == 3) {
                mParent.openUser(holder.mUserAvatar, issue.getOpenedBy().getLogin());
            } else if(url.startsWith("https://github.com/") & url.contains("/issues")) {
                mParent.openIssue(holder.mContent, issue);
            } else {
                final Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                mParent.startActivity(i);
            }
        });
        if(mParseCache.get(pos) == null) {
            final Context context = holder.itemView.getContext();
            final StringBuilder builder = new StringBuilder();
            builder.append("<b>");
            builder.append(issue.getTitle());
            builder.append("</b><br><br>");

            builder.append(String.format(context.getString(R.string.text_opened_by),
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
            final String parsed = MDParser.parseMD(builder.toString());
            mParseCache.set(pos, parsed);
            holder.mContent.setHtml(parsed);
        } else {
            // Log.i(TAG, "onBindViewHolder: Binding pos " + pos + " with\n" + mIssues.get(pos).second);
            holder.mContent.setHtml(mParseCache.get(pos));
        }
    }

    @Override
    public int getItemCount() {
        return mIssues.size();
    }

    private void openIssue(View view, int pos) {
        mParent.openIssue(view, mIssues.get(pos));
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
