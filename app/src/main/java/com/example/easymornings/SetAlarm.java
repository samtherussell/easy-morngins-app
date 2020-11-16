package com.example.easymornings;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;

import androidx.appcompat.app.AppCompatActivity;

public class SetAlarm extends AppCompatActivity {

    TextView alarm;
    TextView before;
    TextView after;
    CompoundButton monday;
    CompoundButton tuesday;
    CompoundButton wednesday;
    CompoundButton thursday;
    CompoundButton friday;
    CompoundButton saturday;
    CompoundButton sunday;
    CompoundButton enabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_alarm);

        SharedPreferences sharedPreferences = AppPreferences.getSharePreferences(this);

        alarm = setupTextView(R.id.time, sharedPreferences, AppPreferences.SHARED_PREFERENCES_ALARM_TIME);
        before = setupTextView(R.id.before, sharedPreferences, AppPreferences.SHARED_PREFERENCES_FADE_IN_TIME);
        after = setupTextView(R.id.after, sharedPreferences, AppPreferences.SHARED_PREFERENCES_OFF_TIME);

        enabled = setupCompoundButton(R.id.enabled, sharedPreferences, AppPreferences.SHARED_PREFERENCES_ENABLED);
        monday = setupCompoundButton(R.id.monday, sharedPreferences, AppPreferences.SHARED_PREFERENCES_MONDAY);
        tuesday = setupCompoundButton(R.id.tuesday, sharedPreferences, AppPreferences.SHARED_PREFERENCES_TUESDAY);
        wednesday = setupCompoundButton(R.id.wednesday, sharedPreferences, AppPreferences.SHARED_PREFERENCES_WEDNESDAY);
        thursday = setupCompoundButton(R.id.thursday, sharedPreferences, AppPreferences.SHARED_PREFERENCES_THURSDAY);
        friday = setupCompoundButton(R.id.friday, sharedPreferences, AppPreferences.SHARED_PREFERENCES_FRIDAY);
        saturday = setupCompoundButton(R.id.saturday, sharedPreferences, AppPreferences.SHARED_PREFERENCES_SATURDAY);
        sunday = setupCompoundButton(R.id.sunday, sharedPreferences, AppPreferences.SHARED_PREFERENCES_SUNDAY);
    }

    CompoundButton setupCompoundButton(int id, SharedPreferences sharedPreferences, String sharedPreferenceName) {
        CompoundButton button = findViewById(id);
        button.setChecked(sharedPreferences.getBoolean(sharedPreferenceName, true));
        button.setOnCheckedChangeListener((v, isChecked) -> {
            sharedPreferences.edit().putBoolean(sharedPreferenceName, isChecked).commit();
            resetAlarms(this);
            Toast.makeText(getApplicationContext(), "Alarm Changed", Toast.LENGTH_SHORT).show();
        });
        return button;
    }

    TextView setupTextView(int id, SharedPreferences sharedPreferences, String sharedPreferenceName) {
        TextView view = findViewById(id);
        long timestamp = sharedPreferences.getLong(sharedPreferenceName, 0);
        view.setText(getAbsoluteTimeString(getHour(timestamp), getMinute(timestamp)));
        view.setOnClickListener(v -> openTimePickerDialog(sharedPreferenceName, view));
        return view;
    }

    private void openTimePickerDialog(String sharedPreferenceName, TextView view) {
        SharedPreferences sharedPreferences = AppPreferences.getSharePreferences(this);
        long alarmTime = sharedPreferences.getLong(sharedPreferenceName, 0);
        new TimePickerDialog(SetAlarm.this, (d, hour, min) -> {
            view.setText(getAbsoluteTimeString(hour, min));
            sharedPreferences.edit().putLong(sharedPreferenceName, getTimestamp(hour, min)).commit();
            resetAlarms(this);
            Toast.makeText(getApplicationContext(), "Alarm Changed", Toast.LENGTH_SHORT).show();
        }, getHour(alarmTime), getMinute(alarmTime), true).show();
    }

    public static void resetAlarms(Context context) {
        resetFadeInAlarm(context);
        resetSoundAlarm(context);
        resetOffAlarm(context);
    }

    public static void resetFadeInAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        SharedPreferences sharedPreferences = AppPreferences.getSharePreferences(context);
        long onTime = sharedPreferences.getLong(AppPreferences.SHARED_PREFERENCES_FADE_IN_TIME, 0);
        long alarmTime = sharedPreferences.getLong(AppPreferences.SHARED_PREFERENCES_ALARM_TIME, 0);
        int fadeTime = (int) (alarmTime - onTime);

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.COMMAND_EXTRA, MainActivity.FADE_ON_COMMAND);
        intent.putExtra(MainActivity.FADE_IN_EXTRA, fadeTime);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, MainActivity.FADE_ON_COMMAND, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (sharedPreferences.getBoolean(AppPreferences.SHARED_PREFERENCES_ENABLED, true))
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, getNextAlarmMillis(onTime, context), pendingIntent);
        else
            alarmManager.cancel(pendingIntent);
    }

    public static void resetSoundAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        SharedPreferences sharedPreferences = AppPreferences.getSharePreferences(context);
        long time = sharedPreferences.getLong(AppPreferences.SHARED_PREFERENCES_ALARM_TIME, 0);

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.COMMAND_EXTRA, MainActivity.SOUND_START_COMMAND);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, MainActivity.SOUND_START_COMMAND, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (sharedPreferences.getBoolean(AppPreferences.SHARED_PREFERENCES_ENABLED, true))
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, getNextAlarmMillis(time, context), pendingIntent);
        else
            alarmManager.cancel(pendingIntent);
    }

    public static void resetOffAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        SharedPreferences sharedPreferences = AppPreferences.getSharePreferences(context);
        long time = sharedPreferences.getLong(AppPreferences.SHARED_PREFERENCES_OFF_TIME, 0);

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.COMMAND_EXTRA, MainActivity.ALL_OFF_COMMAND);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, MainActivity.ALL_OFF_COMMAND, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (sharedPreferences.getBoolean(AppPreferences.SHARED_PREFERENCES_ENABLED, true))
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, getNextAlarmMillis(time, context), pendingIntent);
        else
            alarmManager.cancel(pendingIntent);

    }

    public static long getNextAlarmMillis(long timestamp, Context context) {
        int hour = getHour(timestamp);
        int minute = getMinute(timestamp);
        Calendar now = Calendar.getInstance();
        if (now.get(Calendar.HOUR_OF_DAY) > hour || now.get(Calendar.HOUR_OF_DAY) == hour && now.get(Calendar.MINUTE) >= minute) {
            now.add(Calendar.DATE, 1);
        }
        SharedPreferences sharedPreferences = AppPreferences.getSharePreferences(context);
        while (!sharedPreferences.getBoolean(AppPreferences.getDayOfWeekPreferenceName(now.get(Calendar.DAY_OF_WEEK)), true))
            now.add(Calendar.DATE, 1);
        now.set(Calendar.HOUR_OF_DAY, hour);
        now.set(Calendar.MINUTE, minute);
        now.set(Calendar.SECOND, 0);
        return now.getTimeInMillis();
    }

    private static int getMinute(long time) {
        return (int) ((time / 60) % 60);
    }

    private static int getHour(long time) {
        return (int) (time / (60 * 60));
    }

    private static long getTimestamp(int hour, int minute) {
        return (long) (hour * 60 + minute) * 60;
    }

    private String getAbsoluteTimeString(int hour, int minute) {
        return String.format("%02d:%02d", hour, minute);
    }

    private String getTimeString(long seconds) {
        long minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes > 0 && seconds > 0)
            return String.format("%d:%02d", minutes, seconds);
        else if (minutes > 0)
            return String.format("%d %s", minutes, getString(R.string.minute));
        else
            return String.format("%d %s", seconds, getString(R.string.second));
    }


}