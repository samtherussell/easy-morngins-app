package com.example.easymornings;

import android.content.Context;

import com.example.easymornings.db.Alarm;
import com.example.easymornings.db.AlarmRepository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
public class AlarmController {

    final AlarmRepository alarmRepository;
    final AlarmScheduler alarmScheduler;

    @Value
    static class AlarmTime {
        int hour;
        int minute;
    }

    public static AlarmController create(Context context) {
        AlarmRepository alarmRepository = AlarmRepository.create(context);
        AlarmScheduler alarmScheduler = AlarmScheduler.create(context);
        return new AlarmController(alarmRepository, alarmScheduler);
    }

    Optional<Integer> onChangeFadeTime(Alarm alarm) {
        saveAlarmUpdates(alarm);
        return alarmScheduler.scheduleNextFadeIn(alarm);
    }

    Optional<Integer> onChangeAlarmTime(Alarm alarm) {
        saveAlarmUpdates(alarm);
        alarmScheduler.scheduleNextFadeIn(alarm);
        alarmScheduler.scheduleNextOff(alarm);
        return alarmScheduler.scheduleNextAlarm(alarm);
    }

    Optional<Integer> onChangeOffTime(Alarm alarm) {
        saveAlarmUpdates(alarm);
        return alarmScheduler.scheduleNextOff(alarm);
    }

    public Optional<Integer> onChangeEnabled(Alarm alarm) {
        saveAlarmUpdates(alarm);
        alarmScheduler.scheduleNextFadeIn(alarm);
        alarmScheduler.scheduleNextOff(alarm);
        return alarmScheduler.scheduleNextAlarm(alarm);
    }

    public void onChangeAlarmSound(Alarm alarm) {
        saveAlarmUpdates(alarm);
    }

    CompletableFuture<Alarm> getAlarm(int id) {
        return alarmRepository.getAlarm(id);
    }

    List<Alarm> getAlarmsList() {
        return alarmRepository.getAlarms();
    }

    void loadAlarmsFromStore() {
        alarmRepository.loadAlarms();
    }

    void createNewAlarm() {
        Alarm alarm = Alarm.builder().build();
        alarmRepository.addAlarm(alarm);
    }

    void saveAlarmUpdates(Alarm alarm) {
        alarmRepository.saveAlarmUpdates(alarm);
    }

    void removeAlarm(Alarm alarm) {
        alarmRepository.removeAlarm(alarm);
        alarmScheduler.cancelAlarms(alarm);
    }



}
