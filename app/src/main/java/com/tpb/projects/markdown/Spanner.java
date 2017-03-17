package com.tpb.projects.markdown;

import android.content.Context;
import android.graphics.Color;
import android.text.format.DateUtils;

import com.tpb.projects.R;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Label;
import com.tpb.projects.data.models.Milestone;
import com.tpb.projects.data.models.User;
import com.tpb.projects.markdown.Markdown;

/**
 * Created by theo on 17/03/17.
 */

public class Spanner {

    public static StringBuilder buildIssueSpan(Context context, Issue issue,
                                               boolean headerTitle,
                                               boolean showNumberedTitle,
                                               boolean showNumberedLink,
                                               boolean showAssignees,
                                               boolean showClosedAt,
                                               boolean showCommentCount) {
        final StringBuilder builder = new StringBuilder();
        if(headerTitle) {
            builder.append("<h1>");
            if(showNumberedTitle) builder.append(String.format("#%1$s ", issue.getNumber()));
            builder.append(Markdown.escape(issue.getTitle()).replace("\n", "</h1><h1>")); //h1 won't do multiple lines
            builder.append("</h1>");
        } else {
            builder.append("<b>");
            if(showNumberedTitle) builder.append(String.format("#%1$s ", issue.getNumber()));
            builder.append(Markdown.escape(issue.getTitle()));
            builder.append("</b><br><br>");
        }

        if(issue.getBody() != null && issue.getBody().trim().length() > 0) {
            builder.append(Markdown.formatMD(issue.getBody().replaceFirst("\\s++$", ""), issue.getRepoPath()));
            builder.append("\n \n");
        }
        if(showNumberedLink) {
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

        } else {
            builder.append(
                    String.format(
                            context.getString(R.string.text_opened_this_issue),
                            String.format(context.getString(R.string.text_href),
                                    "https://github.com/" + issue.getOpenedBy().getLogin(),
                                    issue.getOpenedBy().getLogin()
                            ),
                            DateUtils.getRelativeTimeSpanString(issue.getCreatedAt())
                    )
            );
        }
        builder.append("<br>");

        if(issue.getLabels() != null && issue.getLabels().length > 0) {
            Label.appendLabels(builder, issue.getLabels(), "   ");
            builder.append("<br>");
        }
        if(showAssignees && issue.getAssignees() != null) {
            builder.append(context.getString(R.string.text_assigned_to));
            builder.append(' ');
            for(User u : issue.getAssignees()) {
                builder.append(String.format(context.getString(R.string.text_md_link),
                        u.getLogin(),
                        u.getHtmlUrl()));
                builder.append(' ');
            }
            builder.append("<br>");
        }
        if(showCommentCount && issue.getComments() > 0) {
            builder.append("<br>");
            builder.append(context.getResources().getQuantityString(R.plurals.text_issue_comment_count, issue.getComments(), issue.getComments()));
        }
        if(showClosedAt && issue.getClosedAt() != 0 && issue.getClosedBy() != null) {
            builder.append("<br>");
            builder.append(String.format(context.getString(R.string.text_closed_by_link),
                    issue.getClosedBy().getLogin(),
                    issue.getClosedBy().getHtmlUrl(),
                    DateUtils.getRelativeTimeSpanString(issue.getClosedAt())));
        }

        return builder;
    }

    public static StringBuilder buildCombinedIssueSpan(Context context, Issue issue) {
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
        return builder;
    }

}
