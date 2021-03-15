package com.example.easymornings;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import androidx.core.app.NotificationCompat;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;

public class NotificationUtils {

    static final String CHANNEL_NAME = "Easy Mornings";
    static final int FADE_ON_RECEIVER_PROBLEM = 0;
    static final int TURN_OFF_RECEIVER_PROBLEM = 1;

    public static void displayProblemNotification(Context context, String text, int id) {
        NotificationUtils.createNotificationChannel(context);
        Notification notification = NotificationUtils.createNotification(context)
                .setContentTitle("Easy Mornings had a problem")
                .setContentText(text)
                .build();
        NotificationUtils.showNotification(context, notification, id);
    }

    public static void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_NAME, CHANNEL_NAME, IMPORTANCE_DEFAULT);
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    public static NotificationCompat.Builder createNotification(Context context) {
        return new NotificationCompat.Builder(context, CHANNEL_NAME)
                .setSmallIcon(R.drawable.ic_alarmclock)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
    }

    public static void showNotification(Context context, Notification notification, int id) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.notify(id, notification);
    }

}
