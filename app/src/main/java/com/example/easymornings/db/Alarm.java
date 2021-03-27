package com.example.easymornings.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Calendar;

import lombok.Builder;
import lombok.ToString;

@Builder
@ToString
@Entity
public class Alarm {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    public Integer fadeOnDelay;
    public Integer alarmTime;
    public Integer offDelay;
    public String alarmSound;

    @ColumnInfo(defaultValue = "true") public boolean enabled;

    @ColumnInfo(defaultValue = "true") public boolean monday;
    @ColumnInfo(defaultValue = "true") public boolean tuesday;
    @ColumnInfo(defaultValue = "true") public boolean wednesday;
    @ColumnInfo(defaultValue = "true") public boolean thursday;
    @ColumnInfo(defaultValue = "true") public boolean friday;
    @ColumnInfo(defaultValue = "true") public boolean saturday;
    @ColumnInfo(defaultValue = "true") public boolean sunday;

    public boolean isDayEnabled(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY:
                return monday;
            case Calendar.TUESDAY:
                return tuesday;
            case Calendar.WEDNESDAY:
                return wednesday;
            case Calendar.THURSDAY:
                return thursday;
            case Calendar.FRIDAY:
                return friday;
            case Calendar.SATURDAY:
                return saturday;
            case Calendar.SUNDAY:
                return sunday;
            default:
                throw new IndexOutOfBoundsException(String.format("%d is not possible day of week", dayOfWeek));
        }
    }
}

