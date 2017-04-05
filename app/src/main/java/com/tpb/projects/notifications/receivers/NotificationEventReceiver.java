package com.tpb.projects.notifications.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.IntRange;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.tpb.projects.notifications.NotificationIntentService;
import com.tpb.projects.util.Logger;

import java.util.Date;

/**
 * Created by theo on 04/04/17.
 */

public class NotificationEventReceiver extends WakefulBroadcastReceiver {

    private static final String ACTION_START_NOTIFICATION_SERVICE = "ACTION_START_NOTIFICATION_SERVICE";

    private static int NOTIFICATIONS_INTERVAL_IN_MINUTES = 2;

    public static void setUpdateInterval(@IntRange(from = 1, to = 60) int minutes) {
        NOTIFICATIONS_INTERVAL_IN_MINUTES = minutes;
    }

    public static void setupAlarm(Context context) {
        final AlarmManager alarmManager = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        final PendingIntent alarmIntent = getStartPendingIntent(context);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                new Date().getTime(),
                NOTIFICATIONS_INTERVAL_IN_MINUTES * 60000,
                alarmIntent
        );
    }

    private static PendingIntent getStartPendingIntent(Context context) {
        final Intent intent = new Intent(context, NotificationEventReceiver.class);
        intent.setAction(ACTION_START_NOTIFICATION_SERVICE);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(ACTION_START_NOTIFICATION_SERVICE.equals(action)) {
            Logger.i(getClass().getSimpleName(),
                    "onReceive from alarm, starting notification service"
            );
            // Start the service, keeping the device awake while it is launching.
            startWakefulService(context,
                    NotificationIntentService.createIntentStartNotificationService(context)
            );
        }

    }
}