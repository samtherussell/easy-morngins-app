package com.example.easymornings.alarmreceiver;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.easymornings.TimeUtils;
import com.example.easymornings.light.LightConnector;
import com.example.easymornings.NotificationUtils;
import com.example.easymornings.preference.AppPreferenceValues;

public class FadeOnReceiver extends AlarmReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        LightConnector lightConnector = new LightConnector(() -> preferencesConnector.getString(AppPreferenceValues.SHARED_PREFERENCES_IP_ADDRESS, ""));

        Log.w("FADE ON TEST", "Before request");

        alarm.thenAccept(a -> {
            Log.w("FADE ON TEST", String.format("Retrieved alarm: %s", a));
            alarmScheduler.scheduleNextFadeIn(a);

            lightConnector.fade(1, a.alarmTime - TimeUtils.getNowTimestamp(), 50).thenAccept((success) -> {
                Log.w("FADE ON TEST", String.format("After request complete: %s", success));
                if (!success)
                    NotificationUtils.displayProblemNotification(context, "Could not fade on", NotificationUtils.FADE_ON_RECEIVER_PROBLEM);
            }).join();


        }).join();

        Log.w("FADE ON TEST", "After request");
    }
}