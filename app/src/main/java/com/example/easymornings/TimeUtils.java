package com.example.easymornings;

import java.util.ArrayList;

class TimeUtils {
    static int getSecond(long time) {
        return (int) (time % 60);
    }

    static int getMinute(long time) {
        return (int) ((time / 60) % 60);
    }

    static int getHour(long time) {
        return (int) (time / (60 * 60));
    }

    static int getDelay(int minute, int second) {
        return (minute * 60 + second);
    }

    static int getTimestamp(int hour, int minute) {
        return (hour * 60 + minute) * 60;
    }

    static int getSecondsUntil(long epocMilli) {
        return (int) ((epocMilli + 500 - System.currentTimeMillis()) / 1000);
    }

    static String getAbsoluteTimeString(int hour, int minute) {
        return String.format("%02d:%02d", hour, minute);
    }

    static String getDelayString(int minute, int second) {
        return String.format("%02dm%02ds", minute, second);
    }

    static String getTimeIntervalString(int seconds) {
        int days = seconds / (60*60*24);
        seconds -= days * (60*60*24);
        int hours = seconds / (60*60);
        seconds -= hours * (60*60);
        int minutes = seconds / 60;
        seconds -= minutes * 60;
        ArrayList<String> strings = new ArrayList<>();
        if (days > 0)
            strings.add(String.format("%s days", days));
        if (hours > 0)
            strings.add(String.format("%s hours", hours));
        if (days == 0)
            strings.add(String.format("%s min", minutes));
        if (days == 0 && hours == 0)
            strings.add(String.format("%s sec", seconds));
        return String.join(", ", strings);
    }

    static String getFadeTimeString(int seconds) {
        int minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes > 0 && seconds > 0)
            return String.format("%d:%02d", minutes, seconds);
        else if (minutes > 0)
            return String.format("%d min", minutes);
        else
            return String.format("%d sec", seconds);
    }
}
