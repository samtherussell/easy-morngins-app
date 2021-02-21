package com.example.easymornings;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SetAlarm extends AppCompatActivity {

    private static final int SOUND_CHOOSER_REQUEST_CODE = 50;
    private static final int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 51;
    TextView alarm;
    TextView onDelay;
    TextView offDelay;
    TextView currentSound;
    CompoundButton monday;
    CompoundButton tuesday;
    CompoundButton wednesday;
    CompoundButton thursday;
    CompoundButton friday;
    CompoundButton saturday;
    CompoundButton sunday;
    CompoundButton enabled;
    Button changeSound;
    AlarmController alarmController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_alarm);


        alarmController = createAlarmController();
        alarm = setupAlarmTimeView();

        onDelay = setupDelayTimeView(R.id.onDelay, "Fade on time", alarmController::getFadeOnDelay, alarmController::setFadeOnDelay);
        offDelay = setupDelayTimeView(R.id.offDelay, "Off timer", alarmController::getOffDelay, alarmController::setOffDelay);

        enabled = setupCompoundButton(R.id.enabled, AppPreferenceValues.SHARED_PREFERENCES_ENABLED);
        monday = setupCompoundButton(R.id.monday, AppPreferenceValues.SHARED_PREFERENCES_MONDAY);
        tuesday = setupCompoundButton(R.id.tuesday, AppPreferenceValues.SHARED_PREFERENCES_TUESDAY);
        wednesday = setupCompoundButton(R.id.wednesday, AppPreferenceValues.SHARED_PREFERENCES_WEDNESDAY);
        thursday = setupCompoundButton(R.id.thursday, AppPreferenceValues.SHARED_PREFERENCES_THURSDAY);
        friday = setupCompoundButton(R.id.friday, AppPreferenceValues.SHARED_PREFERENCES_FRIDAY);
        saturday = setupCompoundButton(R.id.saturday, AppPreferenceValues.SHARED_PREFERENCES_SATURDAY);
        sunday = setupCompoundButton(R.id.sunday, AppPreferenceValues.SHARED_PREFERENCES_SUNDAY);

        currentSound = setupCurrentSound();
        changeSound = setupChangeSound();
    }

    private AlarmController createAlarmController() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        SharedPreferences sharedPreferences = getSharedPreferences(AppPreferenceValues.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        PreferencesConnector preferencesConnector = new SharedPreferencesConnector(sharedPreferences);
        return new AlarmController(alarmManager, preferencesConnector, getApplicationContext());
    }

    private TextView setupAlarmTimeView() {
        TextView view = findViewById(R.id.time);
        view.setText(alarmController.getAlarmTimeString());
        view.setOnClickListener(v -> openAlarmPickerDialog((TextView) v));
        return view;
    }

    private TextView setupDelayTimeView(int id, String title, Supplier<Integer> getter, Consumer<Integer> setter) {
        TextView view = findViewById(id);
        int delay = getter.get();
        view.setText(TimeUtils.getDelayString(TimeUtils.getMinute(delay), TimeUtils.getSecond(delay)));
        view.setOnClickListener(v -> openDelayPickerDialog((TextView) v, title, getter, setter));
        return view;
    }

    private CompoundButton setupCompoundButton(int id, String sharedPreferenceName) {
        CompoundButton button = findViewById(id);
        button.setChecked(alarmController.isEnabled(sharedPreferenceName));
        button.setOnCheckedChangeListener((v, isChecked) -> {
            int delay = alarmController.setEnabled(sharedPreferenceName, isChecked);
            if (delay > 0) {
                String msg = String.format("Alarm will sound in %s", TimeUtils.getTimeIntervalString(delay));
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
        return button;
    }

    private TextView setupCurrentSound() {
        TextView view = findViewById(R.id.currentsound);
        String sound = alarmController.getAlarmSound();
        if (sound != null) {
            Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), Uri.parse(sound));
            sound = ringtone.getTitle(this);
        }
        view.setText(sound);
        return view;
    }

    private Button setupChangeSound() {
        Button button = findViewById(R.id.changesound);
        button.setOnClickListener(v -> onChangeSound());
        return button;
    }

    private void openAlarmPickerDialog(TextView view) {
        AlarmController.AlarmTime alarmTime = alarmController.getAlarmTime();
        new TimePickerDialog(SetAlarm.this, (d, hour, min) -> {
            view.setText(TimeUtils.getAbsoluteTimeString(hour, min));
            int delay = alarmController.setAlarmTime(hour, min);
            if (delay > 0) {
                String msg = String.format("Alarm will sound in %s", TimeUtils.getTimeIntervalString(delay));
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        }, alarmTime.getHour(), alarmTime.getMinute(), true).show();
    }

    private void openDelayPickerDialog(TextView view, String title, Supplier<Integer> getter, Consumer<Integer> setter) {
        final Dialog dialog = new Dialog(SetAlarm.this);
        dialog.setTitle(title);
        dialog.setContentView(R.layout.delay_picker_dialog);
        Button cancel = dialog.findViewById(R.id.dialog_cancel);
        Button save = dialog.findViewById(R.id.dialog_save);
        NumberPicker minutes = dialog.findViewById(R.id.dialog_minutes);
        int delay = getter.get();
        minutes.setMinValue(0);
        minutes.setMaxValue(99);
        minutes.setValue(TimeUtils.getMinute(delay));
        NumberPicker seconds = dialog.findViewById(R.id.dialog_seconds);
        seconds.setMaxValue(59);
        seconds.setMinValue(0);
        seconds.setValue(TimeUtils.getSecond(delay));
        cancel.setOnClickListener(v -> dialog.dismiss());
        save.setOnClickListener(v -> {
            setter.accept(minutes.getValue() * 60 + seconds.getValue());
            view.setText(TimeUtils.getDelayString(minutes.getValue(), seconds.getValue()));
            dialog.dismiss();
        });
        dialog.show();
    }

    private void onChangeSound() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound");
        this.startActivityForResult(intent, SOUND_CHOOSER_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == -1 && requestCode == SOUND_CHOOSER_REQUEST_CODE) {
            final Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                alarmController.setAlarmSound(uri.toString());
                Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), uri);
                String name = ringtone.getTitle(this);
                currentSound.setText(name);
                if (uri.toString().contains("external")) {
                    int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
                    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
                    }
                }
                Toast.makeText(getApplicationContext(), "Alarm Sounds Changed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "Will not be able to play chosen sound", Toast.LENGTH_SHORT).show();
            }
        }
    }

}