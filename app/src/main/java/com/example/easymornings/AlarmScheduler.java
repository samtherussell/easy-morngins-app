package com.example.easymornings;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.easymornings.alarmreceiver.FadeOnReceiver;
import com.example.easymornings.alarmreceiver.TurnOffReceiver;
import com.example.easymornings.db.Alarm;

import java.util.Calendar;
import java.util.Optional;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AlarmScheduler {

    public static final String UID_EXTRA = "uid";
    public static final String COMMAND_EXTRA = "command";
    public static final int SOUND_START_COMMAND = 2;
    public static final int SLEEP_SOUND_COMMAND = 4;
    public static final int ALARM_SLEEP_DELAY = 5 * 60;

    final AlarmManager alarmManager;
    final Context context;

    public static AlarmScheduler create(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return new AlarmScheduler(alarmManager, context);
    }

    static int getFadeOnRequestCode(int uid) {
        return uid * 10;
    }

    static int getTurnOffRequestCode(int uid) {
        return uid * 10 + 1;
    }

    static int getSleepRequestCode(int uid) {
        return uid * 10 + 2;
    }

    static int getAlarmSoundRequestCode(int uid) {
        return uid * 10 + 3;
    }

    void cancelAlarms(Alarm alarm) {
        if (alarm.fadeOnDelay != null) {
            cancelBroadcastAlarm(new Intent(context, FadeOnReceiver.class), getFadeOnRequestCode(alarm.uid));
        }
        if (alarm.offDelay != null) {
            cancelBroadcastAlarm(new Intent(context, TurnOffReceiver.class), getTurnOffRequestCode(alarm.uid));
        }
        if (alarm.alarmTime != null) {
            cancelActivityAlarm(new Intent(context, AlarmActivity.class), getAlarmSoundRequestCode(alarm.uid));
            cancelSleepAlarm(alarm);
        }
    }

    void cancelBroadcastAlarm(Intent intent, int requestCode) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_NO_CREATE);
        if (pendingIntent != null)
            alarmManager.cancel(pendingIntent);
    }

    void cancelActivityAlarm(Intent intent, int requestCode) {
        PendingIntent pendingIntent = PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_NO_CREATE);
        if (pendingIntent != null)
            alarmManager.cancel(pendingIntent);
    }

    boolean isFadeOnScheduled(Alarm alarm) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, getFadeOnRequestCode(alarm.uid), new Intent(context, FadeOnReceiver.class), PendingIntent.FLAG_NO_CREATE);
        return pendingIntent != null;
    }

    boolean isTurnOffScheduled(Alarm alarm) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, getTurnOffRequestCode(alarm.uid), new Intent(context, TurnOffReceiver.class), PendingIntent.FLAG_NO_CREATE);
        return pendingIntent != null;
    }

    boolean isAlarmScheduled(Alarm alarm) {
        PendingIntent pendingIntent = PendingIntent.getActivity(context, getAlarmSoundRequestCode(alarm.uid), new Intent(context, AlarmActivity.class), PendingIntent.FLAG_NO_CREATE);
        return pendingIntent != null;
    }

    public Optional<Integer> scheduleNextFadeIn(Alarm alarm) {
        Intent intent = new Intent(context, FadeOnReceiver.class);
        intent.putExtra(UID_EXTRA, alarm.uid);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, getFadeOnRequestCode(alarm.uid), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (alarm.fadeOnDelay != null && alarm.alarmTime != null && alarm.enabled && alarm.anyDayEnabled()) {
            int onTime = alarm.alarmTime - alarm.fadeOnDelay;
            Long nextAlarmMillis = getNextAlarmMillis(onTime, alarm::isDayEnabled);
            Log.println(Log.DEBUG, "AlarmScheduler", "scheduleNextFadeIn - set");
            Log.println(Log.DEBUG, "AlarmScheduler", pendingIntent.toString());
            Log.println(Log.DEBUG, "AlarmScheduler", Long.toString(nextAlarmMillis));
            alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(nextAlarmMillis, null), pendingIntent);
            return Optional.of(TimeUtils.getSecondsUntil(nextAlarmMillis));
        } else {
            Log.println(Log.DEBUG, "AlarmScheduler", "scheduleNextFadeIn - remove");
            Log.println(Log.DEBUG, "AlarmScheduler", pendingIntent.toString());
            alarmManager.cancel(pendingIntent);
            return Optional.empty();
        }
    }

    public Optional<Integer> scheduleNextAlarm(Alarm alarm) {
        Intent intent = new Intent(context, AlarmActivity.class);
        intent.putExtra(COMMAND_EXTRA, SOUND_START_COMMAND);
        intent.putExtra("uid", alarm.uid);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, getAlarmSoundRequestCode(alarm.uid), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (alarm.enabled && alarm.alarmTime != null && alarm.anyDayEnabled()) {
            Long nextAlarmMillis = getNextAlarmMillis(alarm.alarmTime, alarm::isDayEnabled);
            Log.println(Log.DEBUG, "AlarmScheduler", "scheduleNextAlarm");
            Log.println(Log.DEBUG, "AlarmScheduler", pendingIntent.toString());
            Log.println(Log.DEBUG, "AlarmScheduler", Long.toString(nextAlarmMillis));
            alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(nextAlarmMillis, null), pendingIntent);
            return Optional.of(TimeUtils.getSecondsUntil(nextAlarmMillis));
        } else {
            Log.println(Log.DEBUG, "AlarmScheduler", "scheduleNextAlarm - remove");
            Log.println(Log.DEBUG, "AlarmScheduler", pendingIntent.toString());
            alarmManager.cancel(pendingIntent);
            return Optional.empty();
        }
    }

    public Optional<Integer> scheduleNextOff(Alarm alarm) {
        Intent intent = new Intent(context, TurnOffReceiver.class);
        intent.putExtra("uid", alarm.uid);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, getTurnOffRequestCode(alarm.uid), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (alarm.offDelay != null && alarm.alarmTime != null && alarm.enabled && alarm.anyDayEnabled()) {
            int offTime = alarm.alarmTime + alarm.offDelay;
            Long nextAlarmMillis = getNextAlarmMillis(offTime, alarm::isDayEnabled);
            Log.println(Log.DEBUG, "AlarmScheduler", "scheduleNextOff");
            Log.println(Log.DEBUG, "AlarmScheduler", pendingIntent.toString());
            Log.println(Log.DEBUG, "AlarmScheduler", Long.toString(nextAlarmMillis));
            alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(nextAlarmMillis, null), pendingIntent);
            return Optional.of(TimeUtils.getSecondsUntil(nextAlarmMillis));
        } else {
            Log.println(Log.DEBUG, "AlarmScheduler", "scheduleNextOff - remove");
            Log.println(Log.DEBUG, "AlarmScheduler", pendingIntent.toString());
            alarmManager.cancel(pendingIntent);
            return Optional.empty();
        }
    }

    public Long getNextAlarmMillis(int timestamp, Function<Integer, Boolean> isDayEnabled) {
        int hour = TimeUtils.getHour(timestamp);
        int minute = TimeUtils.getMinute(timestamp);
        int second = TimeUtils.getSecond(timestamp);
        Calendar now = Calendar.getInstance();
        if (
                now.get(Calendar.HOUR_OF_DAY) > hour ||
                        now.get(Calendar.HOUR_OF_DAY) == hour && now.get(Calendar.MINUTE) > minute ||
                        now.get(Calendar.HOUR_OF_DAY) == hour && now.get(Calendar.MINUTE) == minute && now.get(Calendar.SECOND) >= second
        ) {
            now.add(Calendar.DATE, 1);
        }
        while (!isDayEnabled.apply(now.get(Calendar.DAY_OF_WEEK))) {
            now.add(Calendar.DATE, 1);
        }
        now.set(Calendar.HOUR_OF_DAY, hour);
        now.set(Calendar.MINUTE, minute);
        now.set(Calendar.SECOND, second);
        return now.getTimeInMillis();
    }

    int scheduleSleepAlarm(Alarm alarm) {
        Intent intent = new Intent(context, AlarmActivity.class);
        intent.putExtra(COMMAND_EXTRA, SLEEP_SOUND_COMMAND);
        intent.putExtra("uid", alarm.uid);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, getSleepRequestCode(alarm.uid), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        long triggerAtMillis = System.currentTimeMillis() + ALARM_SLEEP_DELAY * 1000;
        alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAtMillis, null), pendingIntent);
        return TimeUtils.getSecondsUntil(triggerAtMillis);
    }

    void cancelSleepAlarm(Alarm alarm) {
        cancelActivityAlarm(new Intent(context, AlarmActivity.class), getSleepRequestCode(alarm.uid));
    }
}
