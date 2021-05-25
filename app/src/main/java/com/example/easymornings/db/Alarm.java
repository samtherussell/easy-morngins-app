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

    @ColumnInfo(defaultValue = "true") @Builder.Default public boolean enabled = true;

    @ColumnInfo(defaultValue = "true") @Builder.Default public boolean monday = true;
    @ColumnInfo(defaultValue = "true") @Builder.Default public boolean tuesday = true;
    @ColumnInfo(defaultValue = "true") @Builder.Default public boolean wednesday = true;
    @ColumnInfo(defaultValue = "true") @Builder.Default public boolean thursday = true;
    @ColumnInfo(defaultValue = "true") @Builder.Default public boolean friday = true;
    @ColumnInfo(defaultValue = "true") @Builder.Default public boolean saturday = true;
    @ColumnInfo(defaultValue = "true") @Builder.Default public boolean sunday = true;

    public boolean anyDayEnabled() {
        return monday || tuesday || wednesday || thursday || friday || saturday || sunday;
    }

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

