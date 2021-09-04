package com.example.easymornings;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.easymornings.db.Alarm;

import java.util.Calendar;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class AlarmArrayAdapter extends ArrayAdapter<Alarm> {

    final Activity activity;
    final AlarmController controller;

    public AlarmArrayAdapter(@NonNull Activity activity, AlarmController controller) {
        super(activity, R.layout.alarm_details, controller.getAlarmsList());
        this.controller = controller;
        this.activity = activity;
    }

    public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
        Alarm item = getItem(position);
        view = LayoutInflater.from(getContext()).inflate(R.layout.alarm_details, parent, false);
        Log.println(Log.DEBUG, "debug", item.toString());
        setUpRemoveAlarmButton(view, item);
        setupAlarmTimeView(view, item);
        setupFadeOnTimeView(view, item);
        setupTurnOffTimeView(view, item);
        setupCurrentSound(view, item);
        setupChangeSound(view, position);
        setupCompoundButton(view, item, R.id.enabled, a -> a.enabled, (a, c) -> {a.enabled = c; controller.onChangeEnabled(a).ifPresent(notifyTimeUntil()); });
        setupCompoundButton(view, item, R.id.monday, a -> a.monday, (a, c) -> {a.monday = c; controller.onChangeEnabled(a).ifPresent(notifyTimeUntil()); });
        setupCompoundButton(view, item, R.id.tuesday, a -> a.tuesday, (a, c) -> {a.tuesday = c; controller.onChangeEnabled(a).ifPresent(notifyTimeUntil()); });
        setupCompoundButton(view, item, R.id.wednesday, a -> a.wednesday, (a, c) -> {a.wednesday = c; controller.onChangeEnabled(a).ifPresent(notifyTimeUntil()); });
        setupCompoundButton(view, item, R.id.thursday, a -> a.thursday, (a, c) -> {a.thursday = c; controller.onChangeEnabled(a).ifPresent(notifyTimeUntil()); });
        setupCompoundButton(view, item, R.id.friday, a -> a.friday, (a, c) -> {a.friday = c; controller.onChangeEnabled(a).ifPresent(notifyTimeUntil()); });
        setupCompoundButton(view, item, R.id.saturday, a -> a.saturday, (a, c) -> {a.saturday = c; controller.onChangeEnabled(a).ifPresent(notifyTimeUntil()); });
        setupCompoundButton(view, item, R.id.sunday, a -> a.sunday, (a, c) -> {a.sunday = c; controller.onChangeEnabled(a).ifPresent(notifyTimeUntil()); });
        return view;
    }

    private void setUpRemoveAlarmButton(View parent, Alarm alarm) {
        Button delete = parent.findViewById(R.id.delete_alarm);
        delete.setOnClickListener(v -> {
            controller.removeAlarm(alarm);
            notifyDataSetChanged();
        });
    }

    private void setupAlarmTimeView(View parent, Alarm alarm) {
        TextView view = parent.findViewById(R.id.time);

        final String timeString;
        if (alarm.alarmTime == null)
            timeString = "None";
        else
            timeString = TimeUtils.getAbsoluteTimeString(TimeUtils.getHour(alarm.alarmTime), TimeUtils.getMinute(alarm.alarmTime));
        view.setText(timeString);

        final Supplier<Integer> hour = () -> {
            if (alarm.alarmTime == null)
                return Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            else
                return TimeUtils.getHour(alarm.alarmTime);
        };
        final Supplier<Integer> minute = () -> {
            if (alarm.alarmTime == null)
                return Calendar.getInstance().get(Calendar.MINUTE);
            else
                return TimeUtils.getMinute(alarm.alarmTime);
        };
        view.setOnClickListener(v -> new TimePickerDialog(getContext(),
                (d, newHour, newMin) -> {
                    view.setText(TimeUtils.getAbsoluteTimeString(newHour, newMin));
                    alarm.alarmTime = TimeUtils.getTimestamp(newHour, newMin);
                    controller.onChangeAlarmTime(alarm).ifPresent(notifyTimeUntil());
                },
                hour.get(), minute.get(), true).show()
        );
}

    private void setupFadeOnTimeView(View parent, Alarm alarm) {
        TextView view = parent.findViewById(R.id.onDelay);

        final String timeString;
        if (alarm.fadeOnDelay == null)
            timeString = "None";
        else
            timeString = TimeUtils.getDelayString(TimeUtils.getMinute(alarm.fadeOnDelay), TimeUtils.getSecond(alarm.fadeOnDelay));
        view.setText(timeString);

        final Supplier<Integer> getDelay = () -> {
            if (alarm.fadeOnDelay == null)
                return 0;
            else
                return alarm.fadeOnDelay;
        };
        view.setOnClickListener(v -> openDelayPickerDialog(getContext(), "Fade on", getDelay, (minute, second) -> {
            view.setText(TimeUtils.getDelayString(minute, second));
            alarm.fadeOnDelay = TimeUtils.getDelay(minute, second);
            controller.onChangeFadeTime(alarm).ifPresent(notifyTimeUntil("Fade on"));
        }));
    }

    private void setupTurnOffTimeView(View parent, Alarm alarm) {
        TextView view = parent.findViewById(R.id.offDelay);

        final String timeString;
        if (alarm.offDelay == null)
            timeString = "None";
        else
            timeString = TimeUtils.getDelayString(TimeUtils.getMinute(alarm.offDelay), TimeUtils.getSecond(alarm.offDelay));
        view.setText(timeString);

        final Supplier<Integer> getDelay = () -> {
            if (alarm.offDelay == null)
                return 0;
            else
                return alarm.offDelay;
        };
        view.setOnClickListener(v -> openDelayPickerDialog(getContext(), "Turn off", getDelay, (minute, second) -> {
            view.setText(TimeUtils.getDelayString(minute, second));
            alarm.offDelay = TimeUtils.getDelay(minute, second);
            controller.onChangeOffTime(alarm).ifPresent(notifyTimeUntil("Turn off"));
        }));
    }

    private void setupCompoundButton(View parent, Alarm alarm, int id, Function<Alarm, Boolean> getter, BiConsumer<Alarm, Boolean> setter) {
        CompoundButton button = parent.findViewById(id);
        button.setChecked(getter.apply(alarm));
        button.setOnCheckedChangeListener((v, isChecked) -> setter.accept(alarm, isChecked));
    }

    private void setupCurrentSound(View parent, Alarm alarm) {
        TextView view = parent.findViewById(R.id.currentsound);
        String sound = alarm.alarmSound;
        if (sound == null) {
            sound = "none";
        } else {
            Ringtone ringtone = RingtoneManager.getRingtone(getContext(), Uri.parse(alarm.alarmSound));
            sound = ringtone.getTitle(getContext());
        }
        view.setText(sound);
    }

    private void setupChangeSound(View parent, int position) {
        Button button = parent.findViewById(R.id.changesound);
        button.setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound");
            activity.startActivityForResult(intent, position);
        });
    }

    private void openDelayPickerDialog(Context context, String title, Supplier<Integer> currentDelay, BiConsumer<Integer, Integer> onChange) {
        final Dialog dialog = new Dialog(context);
        dialog.setTitle(title);
        dialog.setContentView(R.layout.delay_time_picker_dialog);
        Button cancel = dialog.findViewById(R.id.dialog_cancel);
        Button save = dialog.findViewById(R.id.dialog_save);
        NumberPicker minutes = dialog.findViewById(R.id.dialog_minutes);
        minutes.setMinValue(0);
        minutes.setMaxValue(99);
        minutes.setValue(TimeUtils.getMinute(currentDelay.get()));
        NumberPicker seconds = dialog.findViewById(R.id.dialog_seconds);
        seconds.setMaxValue(59);
        seconds.setMinValue(0);
        seconds.setValue(TimeUtils.getSecond(currentDelay.get()));
        cancel.setOnClickListener(v -> dialog.dismiss());
        save.setOnClickListener(v -> {
            onChange.accept(minutes.getValue(), seconds.getValue());
            dialog.dismiss();
        });
        dialog.show();
    }

    private Consumer<Integer> notifyTimeUntil() {
        return notifyTimeUntil("Alarm sound");
    }

    private Consumer<Integer> notifyTimeUntil(String message) {
        return (i) -> Toast.makeText(getContext(), String.format("%s in %s", message, TimeUtils.getTimeIntervalString(i)), Toast.LENGTH_SHORT).show();
    }

}
