package com.example.easymornings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

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

        long alarmTime = sharedPreferences.getLong(AppPreferences.SHARED_PREFERENCES_ALARM_TIME, 0);
        alarm = findViewById(R.id.time);
        alarm.setText(getAbsoluteTimeString(alarmTime));
        long fadeInTime = sharedPreferences.getLong(AppPreferences.SHARED_PREFERENCES_FADE_IN_TIME, 0);
        before = findViewById(R.id.before);
        before.setText(getTimeString(alarmTime - fadeInTime));
        long offTime = sharedPreferences.getLong(AppPreferences.SHARED_PREFERENCES_OFF_TIME, 0);
        after = findViewById(R.id.after);
        after.setText(getTimeString(offTime - alarmTime));

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
        button.setOnCheckedChangeListener((v, isChecked) -> sharedPreferences.edit().putBoolean(sharedPreferenceName, isChecked).commit());
        return button;
    }

    String getAbsoluteTimeString(long now) {
        LocalTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(now * 1000), ZoneId.systemDefault()).toLocalTime();
        int hour = time.getHour();
        int minute = time.getMinute();
        return String.format("%02d:%02d", hour, minute);
    }

    String getTimeString(long seconds) {
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