 package com.example.easymornings;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class TurnOffReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        SharedPreferences sharedPreferences = context.getSharedPreferences(AppPreferenceValues.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        PreferencesConnector preferencesConnector = new SharedPreferencesConnector(sharedPreferences);
        AlarmController alarmController = new AlarmController(alarmManager, preferencesConnector, context);

        LightConnector lightConnector = new LightConnector(() -> preferencesConnector.getString(AppPreferenceValues.SHARED_PREFERENCES_IP_ADDRESS, ""));

        lightConnector.setNow(0).thenAccept((success) -> {
            if (!success)
                NotificationUtils.displayProblemNotification(context, "Could not turn off", NotificationUtils.FADE_ON_RECEIVER_PROBLEM);
        });

        alarmController.scheduleNextOff();
    }
}