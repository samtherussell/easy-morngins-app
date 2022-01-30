package com.example.easymornings.alarmreceiver;

import android.content.Context;
import android.content.Intent;

import com.example.easymornings.light.LightConnector;
import com.example.easymornings.NotificationUtils;
import com.example.easymornings.preference.AppPreferenceValues;

public class FadeOnReceiver extends AlarmReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        LightConnector lightConnector = new LightConnector(() -> preferencesConnector.getString(AppPreferenceValues.SHARED_PREFERENCES_IP_ADDRESS, ""));

        alarm.thenAccept(a -> {
            lightConnector.fade(1, a.fadeOnDelay, 50).thenAccept((success) -> {
                if (!success)
                    NotificationUtils.displayProblemNotification(context, "Could not fade on", NotificationUtils.FADE_ON_RECEIVER_PROBLEM);
            });

            alarmScheduler.scheduleNextFadeIn(a);
        });
    }
}