package com.tpb.projects.markdown;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.style.LineBackgroundSpan;
import android.text.style.TypefaceSpan;
import android.text.util.Linkify;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tpb.github.data.models.Issue;
import com.tpb.github.data.models.Label;
import com.tpb.github.data.models.Milestone;
import com.tpb.github.data.models.User;
import com.tpb.mdtext.Markdown;
import com.tpb.projects.R;
import com.tpb.projects.common.NetworkImageView;
import com.tpb.projects.util.UI;
import com.tpb.projects.util.Util;

import java.util.Date;

import butterknife.ButterKnife;

import static com.tpb.mdtext.TextUtils.getTextColorForBackground;

/**
 * Created by theo on 17/03/17.
 */

public class Spanner {

    public static StringBuilder buildIssueSpan(Context context, Issue issue,
                                               boolean headerTitle,
                                               boolean showNumberedLink,
                                               boolean showAssignees,
                                               boolean showClosedAt,
                                               boolean showCommentCount) {
        final StringBuilder builder = new StringBuilder();
        if(headerTitle) {
            builder.append("<h1>");
            builder.append(Markdown.escape(issue.getTitle())
                                   .replace("\n", "</h1><h1>")); //h1 won't do multiple lines
            builder.append("</h1>");
        }

        if(issue.getBody() != null && issue.getBody().trim().length() > 0) {
            builder.append(Markdown.formatMD(issue.getBody().replaceFirst("\\s++$", ""),
                    issue.getRepoFullName()
            ));
        }
        builder.append("\n\n");
        if(showNumberedLink) {
            builder.append(String.format(context.getString(R.string.text_issue_opened_by),
                    String.format(context.getString(R.string.text_md_link),
                            "#" + Integer.toString(issue.getNumber()),
                            "https://github.com/" + issue.getRepoFullName() + "/issues/" + Integer
                                    .toString(issue.getNumber())
                    ),
                    String.format(context.getString(R.string.text_md_link),
                            issue.getOpenedBy().getLogin(),
                            issue.getOpenedBy().getHtmlUrl()
                    ),
                    DateUtils.getRelativeTimeSpanString(issue.getCreatedAt())
                    )
            );
            builder.append("<br>");
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
            builder.append("<br>");
        }

        if(issue.getLabels() != null && issue.getLabels().length > 0) {
            appendLabels(builder, issue.getLabels(), "&nbsp;");
            builder.append("<br>");
        }
        if(showAssignees && issue.getAssignees() != null) {
            builder.append(context.getString(R.string.text_assigned_to));
            builder.append(' ');
            for(User u : issue.getAssignees()) {
                builder.append(String.format(context.getString(R.string.text_md_link),
                        u.getLogin(),
                        u.getHtmlUrl()
                ));
                builder.append(' ');
            }
            builder.append("<br>");
        }
        if(showCommentCount && issue.getComments() > 0) {
            builder.append(context.getResources()
                                  .getQuantityString(R.plurals.text_issue_comment_count,
                                          issue.getComments(), issue.getComments()
                                  ));
            builder.append("<br>");
        }
        if(showClosedAt && issue.getClosedAt() != 0 && issue.getClosedBy() != null) {
            builder.append(String.format(context.getString(R.string.text_closed_by_link),
                    issue.getClosedBy().getLogin(),
                    issue.getClosedBy().getHtmlUrl(),
                    DateUtils.getRelativeTimeSpanString(issue.getClosedAt())
            ));
            builder.append("<br>");
        }
        return builder;
    }

    public static StringBuilder buildCombinedIssueSpan(Context context, Issue issue) {
        final StringBuilder builder = new StringBuilder();
        builder.append(String.format(context.getString(R.string.text_issue_opened_by),
                String.format(context.getString(R.string.text_md_link),
                        "#" + Integer.toString(issue.getNumber()),
                        "https://github.com/" + issue.getRepoFullName() + "/issues/" + Integer
                                .toString(issue.getNumber())
                ),
                String.format(context.getString(R.string.text_md_link),
                        issue.getOpenedBy().getLogin(),
                        issue.getOpenedBy().getHtmlUrl()
                ),
                DateUtils.getRelativeTimeSpanString(issue.getCreatedAt())
                )
        );

        if(issue.getAssignees() != null) {
            builder.append("<br>");
            builder.append(context.getString(R.string.text_assigned_to));
            builder.append(' ');
            for(User u : issue.getAssignees()) {
                builder.append(String.format(context.getString(R.string.text_md_link),
                        u.getLogin(),
                        u.getHtmlUrl()
                ));
                builder.append(' ');
            }
        }

        if(issue.isClosed() && issue.getClosedBy() != null) {
            builder.append("<br>");
            builder.append(String.format(context.getString(R.string.text_closed_by_link),
                    issue.getClosedBy().getLogin(),
                    issue.getClosedBy().getHtmlUrl(),
                    DateUtils.getRelativeTimeSpanString(issue.getClosedAt())
            ));
        }

        if(issue.getLabels() != null && issue.getLabels().length > 0) {
            builder.append("<br>");
            appendLabels(builder, issue.getLabels(), "&nbsp;");
        }
        if(issue.getComments() > 0) {
            builder.append("<br>");
            builder.append(context.getResources()
                                  .getQuantityString(R.plurals.text_issue_comment_count,
                                          issue.getComments(), issue.getComments()
                                  ));
        }
        if(issue.getMilestone() != null) {
            final Milestone milestone = issue.getMilestone();
            builder.append("<br>");
            if(milestone.getClosedAt() == 0) {


                if(milestone.getDueOn() > 0) {
                    final StringBuilder dueStringBuilder = new StringBuilder();
                    if(System.currentTimeMillis() < milestone.getDueOn() ||
                            (milestone.getClosedAt() != 0 && milestone.getClosedAt() < milestone
                                    .getDueOn())) {
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
                                            milestone.getTitle()
                                    ),
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
                                            milestone.getTitle()
                                    ),
                                    String.format(
                                            context.getString(R.string.text_milestone_opened),
                                            DateUtils.getRelativeTimeSpanString(
                                                    milestone.getCreatedAt())
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
                                        milestone.getTitle()
                                ),
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

    public static SpannableStringBuilder buildDiffSpan(@NonNull String diff) {
        final SpannableStringBuilder builder = new SpannableStringBuilder();

        int oldLength = 0;
        for(String line : diff.split("\n")) {
            oldLength = builder.length();
            if(line.startsWith("+")) {
                builder.append(line);
                builder.setSpan(new FullWidthBackgroundColorSpan(Color.parseColor("#8BC34A")),
                        oldLength, builder.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            } else if(line.startsWith("-")) {
                builder.append(line);
                builder.setSpan(new FullWidthBackgroundColorSpan(Color.parseColor("#F44336")),
                        oldLength, builder.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            } else {
                builder.append(line);
                builder.setSpan(new FullWidthBackgroundColorSpan(Color.parseColor("#9E9E9E")),
                        oldLength, builder.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
            builder.append("\n");
        }
        builder.setSpan(new TypefaceSpan("monospace"), 0, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        return builder;
    }

    private static class FullWidthBackgroundColorSpan implements LineBackgroundSpan {
        private final int color;

        FullWidthBackgroundColorSpan(int color) {
            this.color = color;
        }

        @Override
        public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline,
                                   int bottom, CharSequence text, int start, int end, int lnum) {
            final int paintColor = p.getColor();
            p.setColor(color);
            p.setAlpha(128);
            c.drawRect(new Rect(left, top, right, bottom), p);
            p.setColor(paintColor);
        }
    }

    public static String bold(String s) {
        return "<b>" + Markdown.escape(s) + "</b>";
    }

    public static String header(String s, @IntRange(from = 0, to = 6) int depth) {
        String header = "<h" + depth + ">";
        header += Markdown.escape(s).replace("\n", "</h" + depth + "><h" + depth + ">");
        return header + "</h" + depth + ">";
    }

    private static void appendLabels(StringBuilder builder, Label[] labels, String spacer) {
        for(Label label : labels) {
            builder.append(getLabelString(label.getName(), label.getColor()));
            builder.append(spacer);
        }
        builder.setLength(Math.max(0, builder.length() - spacer.length())); //Strip the last spacer
    }

    public static String getLabelString(String name, int color) {
        return
                "<font color=\"" +
                        String.format("#%06X", getTextColorForBackground((0xFFFFFF & color))) +
                        "\" bgcolor=\"" +
                        String.format("#%06X", (0xFFFFFF & color)) +
                        "\" " +
                        " rounded=\"true\">" +
                        name +
                        " </font>";
    }
    
    public static void displayUser(LinearLayout userInfoParent, User user) {
        userInfoParent.setVisibility(View.VISIBLE);

        final NetworkImageView avatar = ButterKnife.findById(userInfoParent, R.id.user_avatar);
        avatar.setImageUrl(user.getAvatarUrl());
        final TextView login = ButterKnife.findById(userInfoParent, R.id.user_login);
        login.setText(user.getLogin());

        final Context context = userInfoParent.getContext();
        final LinearLayout infoList = ButterKnife.findById(userInfoParent, R.id.user_info_layout);
        infoList.removeAllViews();
        TextView tv;
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, UI.pxFromDp(4), 0, UI.pxFromDp(4));
        if(user.getName() != null) {
            tv = getInfoTextView(context, R.drawable.ic_person);
            tv.setText(user.getName());
            infoList.addView(tv, params);
        }
        tv = getInfoTextView(context, R.drawable.ic_date);
        tv.setText(
                String.format(
                        context.getString(R.string.text_user_created_at),
                        Util.formatDateLocally(
                                context,
                                new Date(user.getCreatedAt())
                        )
                )
        );
        infoList.addView(tv, params);
        if(user.getEmail() != null) {
            tv = getInfoTextView(context, R.drawable.ic_email);
            tv.setAutoLinkMask(Linkify.EMAIL_ADDRESSES);
            tv.setText(user.getEmail());
            infoList.addView(tv, params);
        }
        if(user.getBlog() != null) {
            tv = getInfoTextView(context, R.drawable.ic_blog);
            tv.setAutoLinkMask(Linkify.WEB_URLS);
            tv.setText(user.getBlog());
            infoList.addView(tv, params);
        }
        if(user.getCompany() != null) {
            tv = getInfoTextView(context, R.drawable.ic_company);
            tv.setText(user.getCompany());
            infoList.addView(tv, params);
        }
        if(user.getLocation() != null) {
            tv = getInfoTextView(context, R.drawable.ic_location);
            tv.setText(user.getLocation());
            infoList.addView(tv, params);
        }
        if(user.getRepos() > 0) {
            tv = getInfoTextView(context, R.drawable.ic_repo);
            tv.setText(context.getResources().getQuantityString(
                    R.plurals.text_user_repositories,
                    user.getRepos(),
                    user.getRepos()
                    )
            );
            infoList.addView(tv, params);
        }
        if(user.getGists() > 0) {
            tv = getInfoTextView(context, R.drawable.ic_gist);
            tv.setText(context.getResources().getQuantityString(
                    R.plurals.text_user_gists,
                    user.getGists(),
                    user.getGists()
                    )
            );
            infoList.addView(tv, params);
        }
        UI.expand(infoList);
    }

    private static TextView getInfoTextView(Context context, @DrawableRes int drawableRes) {
        final TextView tv = new TextView(context);
        tv.setCompoundDrawablePadding(UI.pxFromDp(4));
        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(drawableRes, 0, 0, 0);
        return tv;
    }
    
}
