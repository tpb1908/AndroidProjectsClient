package com.tpb.projects.util;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.View;

import com.androidnetworking.widget.ANImageView;
import com.tpb.projects.R;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.issues.IssueActivity;
import com.tpb.projects.user.UserActivity;

import org.sufficientlysecure.htmltextview.HtmlTextView;

/**
 * Created by theo on 24/02/17.
 */

public class IntentHandler {


    public static void addGitHubIntentHandler(Activity activity, HtmlTextView tv) {
        tv.setLinkClickHandler(url -> {
            if(url.startsWith("https://github.com/") && Data.instancesOf(url, "/") == 3) {
                openUser(activity, tv, url.substring(url.lastIndexOf('/') + 1));
            } else if(url.startsWith("https://github.com/") & url.contains("/issues")) {
                openIssue(activity, tv, url);
            } else {
                final Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                activity.startActivity(i);
            }
        });
    }

    public static void addGitHubIntentHandler(Activity activity, ANImageView iv, String login) {
        iv.setOnClickListener(v -> openUser(activity, iv, login));
    }

    public static void addGitHubIntentHandler(Activity activity, HtmlTextView tv, ANImageView iv, Issue issue) {
        tv.setLinkClickHandler(url -> {
            if(url.startsWith("https://github.com/") && Data.instancesOf(url, "/") == 3) {
                openUser(activity, iv, issue.getOpenedBy().getLogin());
            } else if(url.startsWith("https://github.com/") & url.contains("/issues")) {
                openIssue(activity, tv, issue);
            } else {
                final Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                activity.startActivity(i);
            }
        });
    }

    public static void addGitHubIntentHandler(Activity activity, HtmlTextView tv, ANImageView iv) {
        tv.setLinkClickHandler(url -> {
            if(url.startsWith("https://github.com/") && Data.instancesOf(url, "/") == 3) {
                openUser(activity, iv, url.substring(url.lastIndexOf('/') + 1));
            } else if(url.startsWith("https://github.com/") & url.contains("/issues")) {
                openIssue(activity, tv, url);
            } else {
                final Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                activity.startActivity(i);
            }
        });
    }

   private static void openIssue(Activity activity, View view, String url) {
       final int number = Integer.parseInt(url.substring(url.lastIndexOf('/') + 1));
       final Intent i = new Intent(activity, IssueActivity.class);
       final String repo = url.substring(url.indexOf("com/") + 4, url.indexOf("/issues"));
       i.putExtra(activity.getString(R.string.intent_repo), repo);
       i.putExtra(activity.getString(R.string.intent_issue_number), number);
       if(view instanceof HtmlTextView) {
           UI.setClickPositionForIntent(activity, i, ((HtmlTextView) view).getLastClickPosition());
       } else {
           UI.setViewPositionForIntent(i, view);
       }
       activity.startActivity(i);
    }

    public static void openIssue(Activity activity, View view, Issue issue) {
        final Intent i = new Intent(activity, IssueActivity.class);
        i.putExtra(activity.getString(R.string.parcel_issue), issue);
        if(view instanceof HtmlTextView) {
            UI.setClickPositionForIntent(activity, i, ((HtmlTextView) view).getLastClickPosition());
        } else {
            UI.setViewPositionForIntent(i, view);
        }
        activity.startActivity(i);
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

    private static void openUser(Activity activity, ANImageView iv, String login) {
        final Intent i = new Intent(activity, UserActivity.class);
        i.putExtra(activity.getString(R.string.intent_username), login);
        if(iv.getDrawable() != null) {
            i.putExtra(activity.getString(R.string.intent_drawable), ((BitmapDrawable) iv.getDrawable()).getBitmap());
        }
        activity.startActivity(i, ActivityOptionsCompat.makeSceneTransitionAnimation(
                activity,
                iv,
                activity.getString(R.string.transition_user_image)
                ).toBundle()
        );
    }

}
