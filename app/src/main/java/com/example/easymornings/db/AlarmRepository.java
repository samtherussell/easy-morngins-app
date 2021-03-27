package com.example.easymornings.db;

import android.content.Context;

import androidx.room.Room;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AlarmRepository {

    private final AlarmDao dao;
    @Getter
    private final List<Alarm> alarms;
    private final Executor executor;

    public static AlarmRepository create(Context context) {
        AlarmRoomDatabase db = Room.databaseBuilder(context, AlarmRoomDatabase.class, AlarmRoomDatabase.DATABASE_NAME).build();
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        return new AlarmRepository(db.alarmDao(), new ArrayList<>(), executorService);
    }

    public void loadAlarms() {
        List<Alarm> all = dao.getAll();
        alarms.addAll(all);
    }

    public CompletableFuture<Void> addAlarm(Alarm alarm) {
        alarms.add(alarm);
        return CompletableFuture.runAsync(() -> alarm.uid = (int) dao.insert(alarm), executor);
    }

    public CompletableFuture<Void> removeAlarm(Alarm alarm) {
        alarms.remove(alarm);
        return CompletableFuture.runAsync(() -> dao.delete(alarm), executor);
    }

    public CompletableFuture<Void> saveAlarmUpdates(Alarm alarm) {
        return CompletableFuture.runAsync(() -> dao.update(alarm), executor);
    }

    public CompletableFuture<Alarm> getAlarm(int id) {
        return CompletableFuture.supplyAsync(() -> dao.get(id), executor);
    }
}
