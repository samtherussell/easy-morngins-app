package com.example.easymornings;

import com.example.easymornings.LightConnector.LightState;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import lombok.Builder;
import lombok.Value;

public class LightManager {

    final LightConnector lightConnector;
    private LightState lightState;
    private int fadeTime;
    private double level;

    final ArrayList<Consumer<State>> fadeTimeSubscribers = new ArrayList<>();
    final ArrayList<Consumer<State>> lightStateSubscribers = new ArrayList<>();
    final ArrayList<Consumer<State>> lightLevelSubscribers = new ArrayList<>();
    final ArrayList<Runnable> actionFailedSubscribers = new ArrayList<>();

    @Value @Builder
    static class State {
        LightState lightState;
        int fadeTime;
        double level;
    }

    LightManager(LightConnector lightConnector) {
        this.lightConnector = lightConnector;
        this.fadeTime = 0;
        this.lightState = LightState.UNDEFINED;
        this.level = 0;
    }

    void addFadeTime(int amount) {
        fadeTime += amount;
        State state = getState();
        fadeTimeSubscribers.forEach(sub -> sub.accept(state));
    }

    public void checkLightState() {
        lightConnector.getLightStatus().thenAccept(status -> {
            boolean diffLightState = lightState != status.getLightState();
            boolean diffLightLevel = level != status.getLightLevel();
            level = status.getLightLevel();
            lightState = status.getLightState();
            State state = getState();
            if (diffLightState)
                lightStateSubscribers.forEach(sub -> sub.accept(state));
            if (diffLightLevel)
                lightLevelSubscribers.forEach(sub -> sub.accept(state));
        });
    }

    CompletableFuture<Boolean> on() {
        return on(1);
    }

    CompletableFuture<Boolean> on(float level) {
        if (fadeTime == 0)
            return onNow(level);
        else
            return fadeOnNow(fadeTime);
    }

    CompletableFuture<Boolean> onNow(float level) {
        return lightConnector.onNow(level).thenApply((success) -> {
            if (success) {
                this.fadeTime = 0;
                this.lightState = LightState.ON;
                this.level = level;
                State state = getState();
                fadeTimeSubscribers.forEach(sub -> sub.accept(state));
                lightStateSubscribers.forEach(sub -> sub.accept(state));
                lightLevelSubscribers.forEach(sub -> sub.accept(state));
            } else
                actionFailedSubscribers.forEach(Runnable::run);
            return success;
        });
    }

    CompletableFuture<Boolean> fadeOnNow(int period) {
        return lightConnector.fadeOnNow(period).thenApply((success) -> {
            if (success) {
                this.fadeTime = 0;
                this.lightState = LightState.FADING_ON;
                State state = getState();
                fadeTimeSubscribers.forEach(sub -> sub.accept(state));
                lightStateSubscribers.forEach(sub -> sub.accept(state));
            } else
                actionFailedSubscribers.forEach(Runnable::run);
            return success;
        });
    }

    CompletableFuture<Boolean> off() {
        if (fadeTime == 0)
            return offNow();
        else
            return fadeOffNow(fadeTime);
    }

    CompletableFuture<Boolean> offNow() {
        return lightConnector.offNow().thenApply((success) -> {
            if (success) {
                this.fadeTime = 0;
                this.lightState = LightState.OFF;
                this.level = 0;
                State state = getState();
                fadeTimeSubscribers.forEach(sub -> sub.accept(state));
                lightStateSubscribers.forEach(sub -> sub.accept(state));
                lightLevelSubscribers.forEach(sub -> sub.accept(state));
            } else
                actionFailedSubscribers.forEach(Runnable::run);
            return success;
        });
    }

    CompletableFuture<Boolean> fadeOffNow(int period) {
        return lightConnector.fadeOffNow(period).thenApply((success) -> {
            if (success) {
                this.fadeTime = 0;
                this.lightState = LightState.ON;
                State state = getState();
                fadeTimeSubscribers.forEach(sub -> sub.accept(state));
                lightStateSubscribers.forEach(sub -> sub.accept(state));
                lightLevelSubscribers.forEach(sub -> sub.accept(state));
            } else
                actionFailedSubscribers.forEach(Runnable::run);
            return success;
        });
    }

    private State getState() {
        return State.builder()
                .fadeTime(fadeTime)
                .lightState(lightState)
                .level(level)
                .build();
    }

    void addFadeTimeSubscriber(Consumer<State> subscriber) {
        fadeTimeSubscribers.add(subscriber);
    }

    void addLightStateSubscriber(Consumer<State> subscriber) {
        lightStateSubscribers.add(subscriber);
    }

    void addLightLevelSubscriber(Consumer<State> subscriber) {
        lightLevelSubscribers.add(subscriber);
    }

    void addActionFailedSubscriber(Runnable subscriber) {
        actionFailedSubscribers.add(subscriber);
    }

}
