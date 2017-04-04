package com.tpb.projects.notifications;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.support.v7.app.NotificationCompat;

import com.tpb.github.data.APIHandler;
import com.tpb.github.data.Loader;
import com.tpb.github.data.Util;
import com.tpb.github.data.models.Notification;
import com.tpb.projects.BuildConfig;
import com.tpb.projects.util.Logger;

import java.util.List;

/**
 * Created by theo on 04/04/17.
 */

public class NotificationIntentService extends IntentService implements Loader.ListLoader<Notification> {
    private static final String TAG = NotificationIntentService.class.getSimpleName();

    private static final String ACTION_CHECK = "ACTION_CHECK";

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
            String action = intent.getAction();
            if(ACTION_CHECK.equals(action)) {
                processStartNotification();
                loadNotifications();
            }
        } finally {
            WakefulBroadcastReceiver.completeWakefulIntent(intent);
        }
    }

    private void loadNotifications() {
        if(mLoader == null) mLoader = new Loader(getApplicationContext());
        mLoader.loadNotifications(this, mLastLoadedSuccessfully);
    }

    @Override
    public void listLoadComplete(List<Notification> data) {
        Logger.i(TAG, "listLoadComplete: " + data);
        mLastLoadedSuccessfully = Util.getUTCTimeInMillis();
    }

    @Override
    public void listLoadError(APIHandler.APIError error) {
        Logger.e(TAG, "listLoadError: " + error);
    }

    private void processStartNotification() {

        // Do something. For example, fetch fresh data from backend to create a rich notification?

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle("Scheduled Notification")
               .setAutoCancel(true)
               .setColor(Color.RED)
               .setContentText("This notification has been triggered by Notification Service")
               .setSmallIcon(android.R.drawable.ic_dialog_alert);

//        Intent mainIntent = new Intent(this, NotificationActivity.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this,
//                NOTIFICATION_ID,
//                mainIntent,
//                PendingIntent.FLAG_UPDATE_CURRENT);
//        builder.setContentIntent(pendingIntent);
//        builder.setDeleteIntent(NotificationEventReceiver.getDeleteIntent(this));

        final NotificationManager manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1, builder.build());
    }
}
