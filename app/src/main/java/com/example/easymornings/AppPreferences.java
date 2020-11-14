package com.example.easymornings;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {

    public static final String SHARED_PREFERENCES_FILE = "EASY_MORNINGS_SHARED_PREFERENCES";

    public static final String SHARED_PREFERENCES_ALARM_TIME = "SHARED_PREFERENCES_ALARM_TIME";
    public static final String SHARED_PREFERENCES_FADE_IN_TIME = "SHARED_PREFERENCES_FADE_IN_TIME";
    public static final String SHARED_PREFERENCES_OFF_TIME = "SHARED_PREFERENCES_OFF_TIME";
    public static final String SHARED_PREFERENCES_ENABLED = "SHARED_PREFERENCES_ENABLED";
    public static final String SHARED_PREFERENCES_MONDAY = "SHARED_PREFERENCES_MONDAY";
    public static final String SHARED_PREFERENCES_TUESDAY = "SHARED_PREFERENCES_TUESDAY";
    public static final String SHARED_PREFERENCES_WEDNESDAY = "SHARED_PREFERENCES_WEDNESDAY";
    public static final String SHARED_PREFERENCES_THURSDAY = "SHARED_PREFERENCES_THURSDAY";
    public static final String SHARED_PREFERENCES_FRIDAY = "SHARED_PREFERENCES_FRIDAY";
    public static final String SHARED_PREFERENCES_SATURDAY = "SHARED_PREFERENCES_SATURDAY";
    public static final String SHARED_PREFERENCES_SUNDAY = "SHARED_PREFERENCES_SUNDAY";

    public static final String SHARED_PREFERENCES_IP_ADDRESS = "SHARED_PREFERENCES_IP_ADDRESS";


    static SharedPreferences getSharePreferences(Context context) {
        return context.getSharedPreferences(SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
    }

    static String getDayOfWeekPreferenceName(int dayOfWeek) {
        switch (dayOfWeek) {
            case 0:
                return SHARED_PREFERENCES_MONDAY;
            case 1:
                return SHARED_PREFERENCES_TUESDAY;
            case 2:
                return SHARED_PREFERENCES_WEDNESDAY;
            case 3:
                return SHARED_PREFERENCES_THURSDAY;
            case 4:
                return SHARED_PREFERENCES_FRIDAY;
            case 5:
                return SHARED_PREFERENCES_SATURDAY;
            case 6:
                return SHARED_PREFERENCES_SUNDAY;
            default:
                throw new RuntimeException();
        }
    }

}
