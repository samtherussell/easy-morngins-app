package com.example.easymornings;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class FadeOnReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        SharedPreferences sharedPreferences = context.getSharedPreferences(AppPreferenceValues.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        PreferencesConnector preferencesConnector = new SharedPreferencesConnector(sharedPreferences);
        AlarmController alarmController = new AlarmController(alarmManager, preferencesConnector, context);

        int fadeTime = alarmController.getFadeOnDelay();

        LightConnector lightConnector = new LightConnector(() -> preferencesConnector.getString(AppPreferenceValues.SHARED_PREFERENCES_IP_ADDRESS, ""));

        Handler uiHandler = new Handler(Looper.myLooper());

        lightConnector.fade(1, fadeTime).thenAccept((success) -> {
            if (!success)
                uiHandler.post(() -> Toast.makeText(context, "Could not fade on", Toast.LENGTH_LONG).show());
        });

        alarmController.scheduleNextFadeIn();

    }
}