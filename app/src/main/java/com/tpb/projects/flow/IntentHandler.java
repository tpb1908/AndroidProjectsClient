package com.tpb.projects.flow;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.Pair;
import android.support.v7.widget.CardView;
import android.view.View;

import com.tpb.github.data.models.Commit;
import com.tpb.github.data.models.Issue;
import com.tpb.github.data.models.Milestone;
import com.tpb.mdtext.TextUtils;
import com.tpb.mdtext.views.MarkdownTextView;
import com.tpb.projects.R;
import com.tpb.projects.commits.CommitActivity;
import com.tpb.projects.common.NetworkImageView;
import com.tpb.projects.editors.FullScreenDialog;
import com.tpb.projects.editors.MilestoneEditor;
import com.tpb.projects.issues.IssueActivity;
import com.tpb.projects.user.UserActivity;
import com.tpb.projects.util.UI;

/**
 * Created by theo on 24/02/17.
 */

public class IntentHandler {
    private static final String TAG = IntentHandler.class.getSimpleName();

    public static void addOnClickHandler(Activity activity, MarkdownTextView tv) {
        tv.setLinkClickHandler(url -> {
            if(url.startsWith("https://github.com/") && TextUtils.instancesOf(url, "/") == 3) {
                openUser(activity, tv, url.substring(url.lastIndexOf('/') + 1));
            } else if(url.startsWith("https://github.com/") & url.contains("/issues")) {
                openIssue(activity, tv, url);
            } else {
                final Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                activity.startActivity(i);
            }
        });
    }

    public static void addOnClickHandler(Activity activity, NetworkImageView iv, String login) {
        iv.setOnClickListener(v -> openUser(activity, iv, login));
    }

    public static void addOnClickHandler(Activity activity, View view,Issue issue) {
        view.setOnClickListener(v -> openIssue(activity, view, issue));
    }

    public static void addOnClickHandler(Activity activity, MarkdownTextView tv, NetworkImageView iv, @Nullable CardView cv, Issue issue) {
        tv.setLinkClickHandler(url -> {
            if(url.startsWith("https://github.com/") && TextUtils.instancesOf(url, "/") == 3) {
                if(issue.getOpenedBy().getLogin().equals(url.substring(url.lastIndexOf('/') + 1))) {
                    openUser(activity, iv, issue.getOpenedBy().getLogin());
                } else {
                    openUser(activity, tv, url.substring(url.lastIndexOf('/') + 1));
                }
            } else if(url.startsWith("https://github.com/") & url.contains("/issues")) {
                openIssue(activity, cv == null ? tv : cv, issue);
            } else {
                final Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                activity.startActivity(i);
            }
        });
    }

    public static void addOnClickHandler(Activity activity, MarkdownTextView tv, NetworkImageView iv, String login) {
        tv.setLinkClickHandler(url -> {
            if(url.startsWith("https://github.com/") && TextUtils.instancesOf(url, "/") == 3) {
                openUser(activity, iv, login);
            } else if(url.startsWith("https://github.com/") & url.contains("/issues")) {
                openIssue(activity, tv, url);
            } else {
                final Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                activity.startActivity(i);
            }
        });
        addOnClickHandler(activity, iv, login);
    }

    public static void addOnClickHandler(Activity activity, View view, Commit commit) {
        view.setOnClickListener(v -> openCommit(activity, v, commit));
    }

    private static void openCommit(Activity activity, View view, Commit commit) {
        final Intent intent = new Intent(activity, CommitActivity.class);
        intent.putExtra(activity.getString(R.string.parcel_commit), commit);
        intent.putExtra(activity.getString(R.string.transition_card), "");
        activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(
                activity,
                Pair.create(view, activity.getString(R.string.transition_card)),
                UI.getSafeNavigationBarTransitionPair(activity)
                ).toBundle()
        );
    }

    private static void openIssue(Activity activity, View view, String url) {
        final int number = Integer.parseInt(url.substring(url.lastIndexOf('/') + 1));
        final Intent i = new Intent(activity, IssueActivity.class);
        final String repo = url.substring(url.indexOf("com/") + 4, url.indexOf("/issues"));
        i.putExtra(activity.getString(R.string.intent_repo), repo);
        i.putExtra(activity.getString(R.string.intent_issue_number), number);
        if(view instanceof MarkdownTextView) {
            UI.setClickPositionForIntent(activity, i,
                    ((MarkdownTextView) view).getLastClickPosition()
            );
            activity.startActivity(i);
        } else if(view instanceof CardView) {
            activity.startActivity(i, ActivityOptionsCompat.makeSceneTransitionAnimation(
                    activity,
                    Pair.create(view, activity.getString(R.string.transition_card)),
                    UI.getSafeNavigationBarTransitionPair(activity)
                    ).toBundle()
            );
        } else {
            UI.setViewPositionForIntent(i, view);
            activity.startActivity(i);
        }

    }

    public static void openIssue(Activity activity, View view, Issue issue) {
        final Intent i = new Intent(activity, IssueActivity.class);
        i.putExtra(activity.getString(R.string.parcel_issue), issue);
        i.putExtra(activity.getString(R.string.transition_card), "");
        activity.startActivity(i, ActivityOptionsCompat.makeSceneTransitionAnimation(
                activity,
                Pair.create(view, activity.getString(R.string.transition_card)),
                UI.getSafeNavigationBarTransitionPair(activity)
                ).toBundle()
        );


    }

    private static void openUser(Activity activity, View view, String login) {
        final Intent i = new Intent(activity, UserActivity.class);
        i.putExtra(activity.getString(R.string.intent_username), login);
        if(view instanceof MarkdownTextView) {
            UI.setClickPositionForIntent(activity, i,
                    ((MarkdownTextView) view).getLastClickPosition()
            );
        } else {
            UI.setViewPositionForIntent(i, view);
        }
        activity.startActivity(i);
    }

    public static void openUser(Activity activity, NetworkImageView iv, String login) {
        final Intent i = new Intent(activity, UserActivity.class);
        i.putExtra(activity.getString(R.string.intent_username), login);
        UI.setDrawableForIntent(iv, i);
        activity.startActivity(i, ActivityOptionsCompat.makeSceneTransitionAnimation(
                activity,
                iv,
                activity.getString(R.string.transition_user_image)
                ).toBundle()
        );
    }

    public static void openMilestone(Activity activity, View view, Milestone milestone) {
        final Intent i = new Intent(activity, MilestoneEditor.class);
        i.putExtra(activity.getString(R.string.parcel_milestone), milestone);
        if(view instanceof MarkdownTextView) {
            UI.setClickPositionForIntent(activity, i,
                    ((MarkdownTextView) view).getLastClickPosition()
            );
        } else {
            UI.setViewPositionForIntent(i, view);
        }
        activity.startActivityForResult(i, MilestoneEditor.REQUEST_CODE_EDIT_MILESTONE);
    }

    public static String getUserUrl(String login) {
        return "https://github.com/" + login;
    }

    public static void showFullScreen(Context context, String markdown, @Nullable String repo, FragmentManager manager) {
        final FullScreenDialog dialog = new FullScreenDialog();
        final Bundle b = new Bundle();
        b.putString(context.getString(R.string.intent_markdown), markdown);
        b.putString(context.getString(R.string.intent_repo), repo);
        dialog.setArguments(b);
        dialog.show(manager, TAG);
    }

}
