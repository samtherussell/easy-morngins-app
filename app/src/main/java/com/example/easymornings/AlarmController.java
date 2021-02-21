package com.example.easymornings;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Calendar;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
public class AlarmController {

    final AlarmManager alarmManager;
    final PreferencesConnector preferences;
    final Context context;

    @Value
    static class AlarmTime {
        int hour;
        int minute;
    }

    int getFadeOnDelay() {
        return preferences.getInt(AppPreferenceValues.SHARED_PREFERENCES_FADE_IN_TIME, 0);
    }

    long getAlarmTimestamp() {
        return preferences.getLong(AppPreferenceValues.SHARED_PREFERENCES_ALARM_TIME, 0);
    }

    AlarmTime getAlarmTime() {
        long timestamp = getAlarmTimestamp();
        return new AlarmTime(TimeUtils.getHour(timestamp), TimeUtils.getMinute(timestamp));
    }

    String getAlarmTimeString() {
        AlarmTime alarmTime = getAlarmTime();
        return TimeUtils.getAbsoluteTimeString(alarmTime.getHour(), alarmTime.getMinute());
    }

    int getOffDelay() {
        return preferences.getInt(AppPreferenceValues.SHARED_PREFERENCES_OFF_DELAY, 0);
    }

    boolean isEnabled(String preferenceKey) {
        return preferences.getBoolean(preferenceKey, true);
    }

    void setFadeOnDelay(int seconds) {
        preferences.setInt(AppPreferenceValues.SHARED_PREFERENCES_FADE_IN_TIME, seconds);
        scheduleNextFadeIn();
    }

    int setAlarmTime(int hour, int minute) {
        preferences.setLong(AppPreferenceValues.SHARED_PREFERENCES_ALARM_TIME, TimeUtils.getTimestamp(hour, minute));
        scheduleNextFadeIn();
        scheduleNextOff();
        return scheduleNextAlarm();
    }

    void setOffDelay(int seconds) {
        preferences.setInt(AppPreferenceValues.SHARED_PREFERENCES_OFF_DELAY, seconds);
        scheduleNextOff();
    }

    int setEnabled(String preferenceKey, boolean value) {
        preferences.setBoolean(preferenceKey, value);
        scheduleNextFadeIn();
        scheduleNextOff();
        return scheduleNextAlarm();
    }

    public void scheduleNextFadeIn() {
        int fadeDelay = getFadeOnDelay();
        long alarmTime = getAlarmTimestamp();
        int onTime = (int) (alarmTime - fadeDelay);

        Intent intent = new Intent(context, FadeOnReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (fadeDelay == 0 || preferences.getBoolean(AppPreferenceValues.SHARED_PREFERENCES_ENABLED, true)) {
            long nextAlarmMillis = getNextAlarmMillis(onTime);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextAlarmMillis, pendingIntent);
            System.out.println(String.format("Fade in scheduled for %d", nextAlarmMillis));
        } else
            alarmManager.cancel(pendingIntent);
    }

    public int scheduleNextAlarm() {
        long time = getAlarmTimestamp();
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.COMMAND_EXTRA, MainActivity.SOUND_START_COMMAND);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, MainActivity.SOUND_START_COMMAND, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (preferences.getBoolean(AppPreferenceValues.SHARED_PREFERENCES_ENABLED, true)) {
            long nextAlarmMillis = getNextAlarmMillis(time);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextAlarmMillis, pendingIntent);
            System.out.println(String.format("Alarm scheduled for %d", nextAlarmMillis));
            return (int) (nextAlarmMillis - System.currentTimeMillis()) / 1000;
        } else {
            alarmManager.cancel(pendingIntent);
            return -1;
        }
    }

    public void scheduleNextOff() {
        long delay = getOffDelay();
        long alarmTime = getAlarmTimestamp();
        int offTime = (int) (alarmTime + delay);
        System.out.println(String.format("off delay %d + %d = %d", delay, alarmTime, offTime));
        Intent intent = new Intent(context, TurnOffReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (delay == 0 || preferences.getBoolean(AppPreferenceValues.SHARED_PREFERENCES_ENABLED, true)) {
            long nextAlarmMillis = getNextAlarmMillis(offTime);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextAlarmMillis, pendingIntent);
            System.out.println(String.format("off scheduled for %d", nextAlarmMillis));
        } else
            alarmManager.cancel(pendingIntent);
    }

    public long getNextAlarmMillis(long timestamp) {
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
        while (!preferences.getBoolean(AppPreferenceValues.getDayOfWeekPreferenceName(now.get(Calendar.DAY_OF_WEEK)), true))
            now.add(Calendar.DATE, 1);
        now.set(Calendar.HOUR_OF_DAY, hour);
        now.set(Calendar.MINUTE, minute);
        now.set(Calendar.SECOND, second);
        return now.getTimeInMillis();
    }

    void scheduleSleepAlarm(int seconds) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.COMMAND_EXTRA, MainActivity.SLEEP_SOUND_COMMAND);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, MainActivity.SLEEP_SOUND_COMMAND, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + seconds*1000, pendingIntent);
    }

    void cancelSleepAlarm() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.COMMAND_EXTRA, MainActivity.SLEEP_SOUND_COMMAND);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, MainActivity.SLEEP_SOUND_COMMAND, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);
    }

    String getAlarmSound() {
        return preferences.getString(AppPreferenceValues.SHARED_PREFERENCES_SOUND, null);
    }

    void setAlarmSound(String title) {
        preferences.setString(AppPreferenceValues.SHARED_PREFERENCES_SOUND, title);
    }

}
