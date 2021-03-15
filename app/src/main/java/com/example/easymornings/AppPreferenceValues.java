package com.example.easymornings;

import java.util.Calendar;

public class AppPreferenceValues {

    public static final String SHARED_PREFERENCES_FILE = "EASY_MORNINGS_SHARED_PREFERENCES";

    public static final String SHARED_PREFERENCES_ALARM_TIME = "SHARED_PREFERENCES_ALARM_TIME";
    public static final String SHARED_PREFERENCES_FADE_IN_TIME = "SHARED_PREFERENCES_FADE_IN_TIME";
    public static final String SHARED_PREFERENCES_OFF_DELAY = "SHARED_PREFERENCES_OFF_TIME";
    public static final String SHARED_PREFERENCES_ENABLED = "SHARED_PREFERENCES_ENABLED";
    public static final String SHARED_PREFERENCES_MONDAY = "SHARED_PREFERENCES_MONDAY";
    public static final String SHARED_PREFERENCES_TUESDAY = "SHARED_PREFERENCES_TUESDAY";
    public static final String SHARED_PREFERENCES_WEDNESDAY = "SHARED_PREFERENCES_WEDNESDAY";
    public static final String SHARED_PREFERENCES_THURSDAY = "SHARED_PREFERENCES_THURSDAY";
    public static final String SHARED_PREFERENCES_FRIDAY = "SHARED_PREFERENCES_FRIDAY";
    public static final String SHARED_PREFERENCES_SATURDAY = "SHARED_PREFERENCES_SATURDAY";
    public static final String SHARED_PREFERENCES_SUNDAY = "SHARED_PREFERENCES_SUNDAY";

    public static final String SHARED_PREFERENCES_IP_ADDRESS = "SHARED_PREFERENCES_IP_ADDRESS";

    public static final String SHARED_PREFERENCES_SOUND = "SHARED_PREFERENCES_SOUND";

    static String getDayOfWeekPreferenceName(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY:
                return SHARED_PREFERENCES_MONDAY;
            case Calendar.TUESDAY:
                return SHARED_PREFERENCES_TUESDAY;
            case Calendar.WEDNESDAY:
                return SHARED_PREFERENCES_WEDNESDAY;
            case Calendar.THURSDAY:
                return SHARED_PREFERENCES_THURSDAY;
            case Calendar.FRIDAY:
                return SHARED_PREFERENCES_FRIDAY;
            case Calendar.SATURDAY:
                return SHARED_PREFERENCES_SATURDAY;
            case Calendar.SUNDAY:
                return SHARED_PREFERENCES_SUNDAY;
            default:
                throw new IndexOutOfBoundsException(String.format("%d is not possible day of week", dayOfWeek));
        }
    }

}
