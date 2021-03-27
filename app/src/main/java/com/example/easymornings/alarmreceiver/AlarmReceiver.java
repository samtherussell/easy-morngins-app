package com.example.easymornings.alarmreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.example.easymornings.AlarmScheduler;
import com.example.easymornings.preference.AppPreferenceValues;
import com.example.easymornings.NotificationUtils;
import com.example.easymornings.preference.PreferencesConnector;
import com.example.easymornings.preference.SharedPreferencesConnector;
import com.example.easymornings.db.Alarm;
import com.example.easymornings.db.AlarmRepository;

import java.util.concurrent.CompletableFuture;

public class AlarmReceiver extends BroadcastReceiver {

    CompletableFuture<Alarm> alarm;
    AlarmScheduler alarmScheduler;
    PreferencesConnector preferencesConnector;

    @Override
    public void onReceive(Context context, Intent intent) {
        setPreferencesConnector(context);
        setAlarm(context, intent);
        setAlarmScheduler(context);
    }

    private void setAlarmScheduler(Context context) {
        alarmScheduler = AlarmScheduler.create(context);
    }

    private void setAlarm(Context context, Intent intent) {
        AlarmRepository alarmRepository = AlarmRepository.create(context);
        int uid = intent.getIntExtra("uid", -1);
        if (uid == -1) {
            NotificationUtils.displayProblemNotification(context, "Could not fade on", NotificationUtils.FADE_ON_RECEIVER_PROBLEM);
        }
        alarm = alarmRepository.getAlarm(uid);
    }

    private void setPreferencesConnector(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(AppPreferenceValues.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        preferencesConnector = new SharedPreferencesConnector(sharedPreferences);
    }
}
