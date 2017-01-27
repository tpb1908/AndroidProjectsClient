/*
 * Copyright  2016 Theo Pearson-Bray
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.tpb.projects.issues;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.tpb.projects.R;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Label;
import com.tpb.projects.data.models.User;
import com.tpb.projects.util.Data;

import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter;
import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.util.ArrayList;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 27/01/17.
 */

public class IssuesAdapter extends RecyclerView.Adapter<IssuesAdapter.IssueHolder> {
    private static final String TAG = IssuesAdapter.class.getSimpleName();

    private ArrayList<Issue> mIssues = new ArrayList<>();

    public IssuesAdapter() {

    }

    void loadIssues(Issue[] issues) {
        mIssues = new ArrayList<>(Arrays.asList(issues));
        notifyDataSetChanged();
    }

    @Override
    public IssueHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new IssueHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_issue, parent, false));
    }

    @Override
    public void onBindViewHolder(IssueHolder holder, int position) {
        final Issue issue = mIssues.get(position);
        final Context context = holder.itemView.getContext();
        holder.mIssueIcon.setVisibility(View.VISIBLE);
        holder.mIssueIcon.setImageResource(issue.isClosed() ? R.drawable.ic_issue_closed : R.drawable.ic_issue_open);

        final StringBuilder builder = new StringBuilder();
        builder.append("<b>");
        builder.append(issue.getTitle());
        builder.append("</b><br><br>");
        if(issue.getBody() != null && !issue.getBody().isEmpty()) {
            builder.append(Data.formatMD(issue.getBody(), issue.getRepoPath()));
            builder.append("<br>");
        }
        final String html = Data.parseMD(builder.toString(), issue.getRepoPath());

        holder.mContent.setHtml(html,  new HtmlHttpImageGetter(holder.mContent));
        builder.setLength(0);

        if(Data.instancesOf(html, "<br>") + Data.instancesOf(html, "<p>") + 4 >= 15) {
            builder.append("...<br>");
        }
        builder.append("<br>");

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
        holder.mInfo.setHtml(Data.parseMD(builder.toString(), issue.getRepoPath()));
    }

    @Override
    public int getItemCount() {
        return mIssues.size();
    }

    public class IssueHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.issue_content_markdown) HtmlTextView mContent;
        @BindView(R.id.issue_info_markdown) HtmlTextView mInfo;
        @BindView(R.id.issue_menu_button) ImageButton mMenuButton;
        @BindView(R.id.issue_drawable) ImageView mIssueIcon;

        IssueHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            mContent.setShowUnderLines(false);
            mInfo.setShowUnderLines(false);
        }


    }
}
