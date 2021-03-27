package com.example.easymornings.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Alarm.class}, version = 1, exportSchema = false)
public abstract class AlarmRoomDatabase extends RoomDatabase {
    public static final String DATABASE_NAME = "alarm";
    public abstract AlarmDao alarmDao();
}
