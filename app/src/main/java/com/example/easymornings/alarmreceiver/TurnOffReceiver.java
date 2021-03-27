 package com.example.easymornings.alarmreceiver;

 import android.content.Context;
 import android.content.Intent;

 import com.example.easymornings.light.LightConnector;
 import com.example.easymornings.NotificationUtils;
 import com.example.easymornings.preference.AppPreferenceValues;

 public class TurnOffReceiver extends AlarmReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        LightConnector lightConnector = new LightConnector(() -> preferencesConnector.getString(AppPreferenceValues.SHARED_PREFERENCES_IP_ADDRESS, ""));

        lightConnector.setNow(0).thenAccept((success) -> {
            if (!success)
                NotificationUtils.displayProblemNotification(context, "Could not turn off", NotificationUtils.TURN_OFF_RECEIVER_PROBLEM);
        });

        alarm.thenAccept(a -> alarmScheduler.scheduleNextOff(a));
    }
 }