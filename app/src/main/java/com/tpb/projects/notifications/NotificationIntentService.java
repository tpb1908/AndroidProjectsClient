package com.tpb.projects.notifications;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.support.v7.app.NotificationCompat;

import com.tpb.github.data.APIHandler;
import com.tpb.github.data.Editor;
import com.tpb.github.data.Loader;
import com.tpb.github.data.Util;
import com.tpb.github.data.models.Notification;
import com.tpb.mdtext.TextUtils;
import com.tpb.projects.BuildConfig;
import com.tpb.projects.R;
import com.tpb.projects.flow.Interceptor;
import com.tpb.projects.util.Logger;

import java.util.List;

/**
 * Created by theo on 04/04/17.
 */

public class NotificationIntentService extends IntentService implements Loader.ListLoader<Notification> {
    private static final String TAG = NotificationIntentService.class.getSimpleName();

    private static final String ACTION_CHECK = "ACTION_CHECK";
    private static final String ACTION_DELETE = "ACTION_DELETE";

    private Loader mLoader;
    private long mLastLoadedSuccessfully = 0;

    public NotificationIntentService() {
        super(BuildConfig.APPLICATION_ID);
    }

    public static Intent createIntentStartNotificationService(Context context) {
        Intent intent = new Intent(context, NotificationIntentService.class);
        intent.setAction(ACTION_CHECK);
        return intent;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if(intent == null) return;
        try {
            final String action = intent.getAction();
            if(ACTION_CHECK.equals(action)) {
                loadNotifications();
            } else if(ACTION_DELETE.equals(action) && intent.hasExtra("notification")) {
                markNotificationRead(
                        ((Notification) intent.getParcelableExtra("notification")).getId());
            }
        } finally {
            Logger.i(TAG, "onHandleIntent: " + intent.toString());
            WakefulBroadcastReceiver.completeWakefulIntent(intent);
        }
    }

    private void loadNotifications() {
        if(mLoader == null) mLoader = Loader.getLoader(getApplicationContext());
        Logger.i(TAG, "loadNotifications: Timestamp " + Util
                .toISO8061FromMilliseconds(mLastLoadedSuccessfully));
        mLoader.loadNotifications(this, mLastLoadedSuccessfully);
    }

    private void markNotificationRead(long id) {
        Editor.getEditor(this).markNotificationThreadRead(id);
    }

    private android.app.Notification buildNotification(Notification notif) {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        String title;
        switch(notif.getReason()) {
            case AUTHOR:
                title = String.format(getString(R.string.text_notification_author),
                        notif.getRepository().getFullName()
                );
                builder.setSmallIcon(R.drawable.ic_person_white);
                break;
            case COMMENT:
                if("issue".equalsIgnoreCase(notif.getType())) {
                    //TODO Get issue number
                } else if(notif.getUrl().contains("/commits/")) {
                    //TODO get commit ref
                }
                //else
                title = String.format(getString(R.string.text_notification_comment),
                        notif.getRepository().getName()
                );
                builder.setSmallIcon(R.drawable.ic_comment_white);
                break;
            case ASSIGN:
                title = String.format(
                        getString(R.string.text_notification_assign),
                        "#A number",
                        notif.getRepository().getFullName()
                );
                builder.setSmallIcon(R.drawable.ic_person_white);
                break;
            case INVITATION:
                title = getString(R.string.text_notification_invitation);
                builder.setSmallIcon(R.drawable.ic_group_add_white);
                break;
            case MANUAL:
                title = getString(R.string.text_notification_manual,
                        notif.getRepository().getFullName()
                );
                builder.setSmallIcon(R.drawable.ic_watchers_white);
                //TODO get thread
                break;
            case MENTION:
                title = getString(R.string.text_notification_mention,
                        notif.getRepository().getFullName()
                );
                builder.setSmallIcon(R.drawable.ic_mention_white);
                break;
            case SUBSCRIBED:
                if("issue".equalsIgnoreCase(notif.getType())) {
                    title = String.format(getString(R.string.text_notification_issue),
                            notif.getRepository().getName()
                    );
                    builder.setSmallIcon(R.drawable.ic_issue_white);
                } else {
                    title = getString(R.string.text_notification_subscribed,
                            notif.getRepository().getFullName()
                    );
                    builder.setSmallIcon(R.drawable.ic_watchers_white);
                }
                break;
            default:
                title = TextUtils.capitaliseFirst(notif.getReason().toString());
                break;
        }
        final TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(Interceptor.class);
        stackBuilder.addNextIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(notif.getUrl())));
        Logger.i(TAG, "buildNotification: URL " + notif.getUrl());
        builder.setContentIntent(
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT));
        builder.setCategory(android.app.Notification.CATEGORY_MESSAGE);
        builder.setContentTitle(title);
        builder.setContentText(notif.getTitle());
        builder.setAutoCancel(true);
        builder.setDeleteIntent(generateDismissIntent(notif));
        builder.setGroup("GITHUB_GROUP");
        return builder.build();
    }

    private PendingIntent generateDismissIntent(Notification notif) {
        final Intent i = new Intent(NotificationIntentService.this,
                NotificationIntentService.class
        );
        i.setAction(ACTION_DELETE);
        i.putExtra("notification", notif);
        return PendingIntent.getService(this, 53253, i, PendingIntent.FLAG_ONE_SHOT);

    }

    @Override
    public void listLoadComplete(List<Notification> notifications) {
        Logger.i(TAG, "listLoadComplete: " + notifications.size());
        mLastLoadedSuccessfully = Util.getUTCTimeInMillis();
        final NotificationManager manager = (NotificationManager) this
                .getSystemService(Context.NOTIFICATION_SERVICE);
        for(Notification n : notifications) {
            manager.notify((int) n.getId(), buildNotification(n));
        }
    }


    @Override
    public void listLoadError(APIHandler.APIError error) {
        Logger.e(TAG, "listLoadError: " + error);
    }

}
