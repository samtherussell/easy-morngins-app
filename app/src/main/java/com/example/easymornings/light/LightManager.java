package com.example.easymornings.light;

import com.example.easymornings.light.LightConnector.LightStatus;
import com.example.easymornings.light.LightConnector.LightState;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class LightManager {

    final ScheduledExecutorService executorService;
    final int MAX_STATUS_CHECK_DELAY = 2000;
    final int MIN_STATUS_CHECK_DELAY = 100;
    int statusCheckDelay = MIN_STATUS_CHECK_DELAY;
    boolean checkLightStateFlag;

    final LightConnector lightConnector;

    private int delayTime;
    private DelayMode delayMode;
    private LightStatus lightStatus;

    final ArrayList<Consumer<Integer>> delayTimeSubscribers = new ArrayList<>();
    final ArrayList<Consumer<LightStatus>> lightStateSubscribers = new ArrayList<>();
    final ArrayList<Consumer<LightStatus>> lightLevelSubscribers = new ArrayList<>();
    final ArrayList<Consumer<LightStatus>> timeLeftSubscribers = new ArrayList<>();
    final ArrayList<Runnable> actionFailedSubscribers = new ArrayList<>();

    public enum DelayMode {FADE, TIMER};

    public LightManager(LightConnector lightConnector) {
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        this.lightConnector = lightConnector;
        this.delayTime = 0;
        this.delayMode = DelayMode.TIMER;
    }

    public void addDelayTime(int amount) {
        delayTime += amount;
        delayTimeSubscribers.forEach(sub -> sub.accept(delayTime));
    }

    public boolean hasDelayTime() {
        return delayTime > 0;
    }

    public void cancelDelayTime() {
        delayTime = 0;
        delayTimeSubscribers.forEach(sub -> sub.accept(delayTime));
    }

    public void setDelayMode(DelayMode val) {
        delayMode = val;
    }

    public void startCheckLightState() {
        checkLightStateFlag = true;
        checkLightState();
    }

    public void stopCheckLightState() {
        checkLightStateFlag = false;
    }

    public CompletableFuture<Boolean> checkLightState() {
        return lightConnector.getLightStatus().thenApply(status -> {
            boolean diffLightState = this.lightStatus == null || this.lightStatus.getLightState() != status.getLightState();
            boolean diffLightLevel = this.lightStatus == null || this.lightStatus.getLightLevel() != status.getLightLevel();
            boolean diffTimeLeft = this.lightStatus == null || this.lightStatus.getTimeLeft() != status.getTimeLeft();
            this.lightStatus = status;
            if (diffLightState)
                lightStateSubscribers.forEach(sub -> sub.accept(status));
            if (diffLightLevel)
                lightLevelSubscribers.forEach(sub -> sub.accept(status));
            if (diffTimeLeft)
                timeLeftSubscribers.forEach(sub -> sub.accept(status));

            boolean changed = diffLightState || diffLightLevel || diffTimeLeft;
            scheduleNextRun(changed);

            return changed;
        });
    }

    private void scheduleNextRun(boolean changed) {
        if (!checkLightStateFlag)
            return;

        if (changed)
            statusCheckDelay = MIN_STATUS_CHECK_DELAY;
        else
            statusCheckDelay = Math.min(statusCheckDelay * 2, MAX_STATUS_CHECK_DELAY);

        executorService.schedule(this::checkLightState, statusCheckDelay, TimeUnit.MILLISECONDS);
    }

    public CompletableFuture<Boolean> on() {
        return setLevel(1);
    }

    public CompletableFuture<Boolean> off() {
        return setLevel(0);
    }

    public CompletableFuture<Boolean> setLevel(float level) {
        if (delayTime == 0)
            return setNow(level);
        else if (delayMode == DelayMode.FADE)
            return fade(level, delayTime);
        else if (delayMode == DelayMode.TIMER)
            return timer(level, delayTime);
        else
            throw new RuntimeException();
    }

    CompletableFuture<Boolean> setNow(float level) {
        return lightConnector.setNow(level).thenApply((success) -> {
            if (success) {
                cancelDelayTime();
                this.lightStatus = new LightStatus(LightState.CONSTANT, level, -1);
                lightStateSubscribers.forEach(sub -> sub.accept(this.lightStatus));
                lightLevelSubscribers.forEach(sub -> sub.accept(this.lightStatus));
                timeLeftSubscribers.forEach(sub -> sub.accept(this.lightStatus));
            } else
                actionFailedSubscribers.forEach(Runnable::run);
            return success;
        });
    }

    CompletableFuture<Boolean> fade(float level, int period) {
        return lightConnector.fade(level, period).thenApply((success) -> {
            if (success) {
                cancelDelayTime();
                this.lightStatus = new LightStatus(LightState.FADING, level, period);
                lightStateSubscribers.forEach(sub -> sub.accept(this.lightStatus));
                timeLeftSubscribers.forEach(sub -> sub.accept(this.lightStatus));
            } else
                actionFailedSubscribers.forEach(Runnable::run);
            return success;
        });
    }

    CompletableFuture<Boolean> timer(float level, int period) {
        return lightConnector.timer(level, period).thenApply((success) -> {
            if (success) {
                cancelDelayTime();
                this.lightStatus = new LightStatus(LightState.TIMER, level, period);
                timeLeftSubscribers.forEach(sub -> sub.accept(this.lightStatus));
            } else
                actionFailedSubscribers.forEach(Runnable::run);
            return success;
        });
    }

    public void addDelayTimeSubscriber(Consumer<Integer> subscriber) {
        delayTimeSubscribers.add(subscriber);
    }

    public void addLightStateSubscriber(Consumer<LightStatus> subscriber) {
        lightStateSubscribers.add(subscriber);
    }

    public void addLightLevelSubscriber(Consumer<LightStatus> subscriber) {
        lightLevelSubscribers.add(subscriber);
    }

    public void addTimeLeftSubscribers(Consumer<LightStatus> subscriber) {
        timeLeftSubscribers.add(subscriber);
    }

    public void addActionFailedSubscriber(Runnable subscriber) {
        actionFailedSubscribers.add(subscriber);
    }

}
