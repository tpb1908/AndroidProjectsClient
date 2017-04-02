package com.tpb.projects.flow;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.CardView;
import android.view.View;

import com.tpb.github.data.models.Commit;
import com.tpb.github.data.models.Issue;
import com.tpb.github.data.models.Milestone;
import com.tpb.projects.R;
import com.tpb.projects.commits.CommitActivity;
import com.tpb.projects.editors.MilestoneEditor;
import com.tpb.projects.issues.IssueActivity;
import com.tpb.projects.user.UserActivity;
import com.tpb.projects.util.NetworkImageView;
import com.tpb.projects.util.UI;
import com.tpb.projects.util.Util;

import org.sufficientlysecure.htmltext.htmltextview.HtmlTextView;

/**
 * Created by theo on 24/02/17.
 */

public class IntentHandler {
    private static final String TAG = IntentHandler.class.getSimpleName();

    public static void addOnClickHandler(Activity activity, HtmlTextView tv) {
        tv.setLinkClickHandler(url -> {
            if(url.startsWith("https://github.com/") && Util.instancesOf(url, "/") == 3) {
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

    public static void addOnClickHandler(Activity activity, View view, CardView parent, Issue issue) {
        view.setOnClickListener(v -> openIssue(activity, parent, issue));
    }

    public static void addOnClickHandler(Activity activity, HtmlTextView tv, NetworkImageView iv, @Nullable CardView cv, Issue issue) {
        tv.setLinkClickHandler(url -> {
            if(url.startsWith("https://github.com/") && Util.instancesOf(url, "/") == 3) {
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

    public static void addOnClickHandler(Activity activity, HtmlTextView tv, NetworkImageView iv, String login) {
        tv.setLinkClickHandler(url -> {
            if(url.startsWith("https://github.com/") && Util.instancesOf(url, "/") == 3) {
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
        if(view instanceof HtmlTextView) {
            UI.setClickPositionForIntent(activity, i, ((HtmlTextView) view).getLastClickPosition());
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
        if(view instanceof HtmlTextView) {
            UI.setClickPositionForIntent(activity, i, ((HtmlTextView) view).getLastClickPosition());
            activity.startActivity(i);
        } else if(view instanceof CardView) {
            i.putExtra(activity.getString(R.string.transition_card), "");
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

    private static void openUser(Activity activity, View view, String login) {
        final Intent i = new Intent(activity, UserActivity.class);
        i.putExtra(activity.getString(R.string.intent_username), login);
        if(view instanceof HtmlTextView) {
            UI.setClickPositionForIntent(activity, i, ((HtmlTextView) view).getLastClickPosition());
        } else {
            UI.setViewPositionForIntent(i, view);
        }
        activity.startActivity(i);
    }

    public static void openUser(Activity activity, NetworkImageView iv, String login) {
        final Intent i = new Intent(activity, UserActivity.class);
        i.putExtra(activity.getString(R.string.intent_username), login);
        if(iv.getDrawable() != null && iv.getDrawable() instanceof BitmapDrawable) {
            i.putExtra(activity.getString(R.string.intent_drawable),
                    ((BitmapDrawable) iv.getDrawable()).getBitmap()
            );
        }
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
        if(view instanceof HtmlTextView) {
            UI.setClickPositionForIntent(activity, i, ((HtmlTextView) view).getLastClickPosition());
        } else {
            UI.setViewPositionForIntent(i, view);
        }
        activity.startActivityForResult(i, MilestoneEditor.REQUEST_CODE_EDIT_MILESTONE);
    }

    public static String getUserUrl(String login) {
        return "https://github.com/" + login;
    }

}
