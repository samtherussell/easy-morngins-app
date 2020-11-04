package com.example.easymornings;

public class LightConnector {

    enum LightState {OFF, FADING_ON, ON, FADING_OFF};

    public LightState getLightState() {
        return LightState.OFF;
    }

    void onNow() {
        System.out.println("On now");
    }

    void fadeOnNow(int period) {
        long start = now();
        long end = start + period;
        fadeOn(start, end);
    }

    void fadeOn(long start, long end) {
        System.out.println("Fade On");
    }

    void offNow() {
        System.out.println("Off now");
    }

    void fadeOffNow(int period) {
        long start = now();
        long end = start + period;
        fadeOff(start, end);
    }

    void fadeOff(long start, long end) {
        System.out.println("Fade Off");
    }

    static long now() {
        return System.currentTimeMillis()/1000;
    }

}
