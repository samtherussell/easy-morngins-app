package com.example.easymornings.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AlarmDao {

    @Query("SELECT * FROM alarm")
    List<Alarm> getAll();

    @Insert
    long insert(Alarm alarm);

    @Delete
    void delete(Alarm alarm);

    @Update
    void update(Alarm alarm);

    @Query("SELECT * FROM alarm WHERE uid = :id LIMIT 1")
    Alarm get(int id);
}

