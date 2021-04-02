package com.example.easymornings.light;

import com.example.easymornings.light.LightConnector.LightState;

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
    public static class State {
        LightState lightState;
        int fadeTime;
        double level;
    }

    public LightManager(LightConnector lightConnector) {
        this.lightConnector = lightConnector;
        this.fadeTime = 0;
        this.lightState = LightState.UNDEFINED;
        this.level = 0;
    }

    public void addFadeTime(int amount) {
        fadeTime += amount;
        State state = getState();
        fadeTimeSubscribers.forEach(sub -> sub.accept(state));
    }

    public CompletableFuture<Boolean> checkLightState() {
        return lightConnector.getLightStatus().thenApply(status -> {
            boolean diffLightState = lightState != status.getLightState();
            boolean diffLightLevel = level != status.getLightLevel();
            level = status.getLightLevel();
            lightState = status.getLightState();
            State state = getState();
            if (diffLightState)
                lightStateSubscribers.forEach(sub -> sub.accept(state));
            if (diffLightLevel)
                lightLevelSubscribers.forEach(sub -> sub.accept(state));
            return diffLightState || diffLightLevel;
        });
    }

    public CompletableFuture<Boolean> on() {
        return setLevel(1);
    }

    public CompletableFuture<Boolean> off() {
        return setLevel(0);
    }

    public CompletableFuture<Boolean> setLevel(float level) {
        if (fadeTime == 0)
            return setNow(level);
        else
            return fade(level, fadeTime);
    }

    CompletableFuture<Boolean> setNow(float level) {
        return lightConnector.setNow(level).thenApply((success) -> {
            if (success) {
                this.fadeTime = 0;
                this.lightState = LightState.CONSTANT;
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

    CompletableFuture<Boolean> fade(float level, int period) {
        return lightConnector.fade(level, period).thenApply((success) -> {
            if (success) {
                this.fadeTime = 0;
                this.lightState = LightState.FADING;
                State state = getState();
                fadeTimeSubscribers.forEach(sub -> sub.accept(state));
                lightStateSubscribers.forEach(sub -> sub.accept(state));
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

    public void addFadeTimeSubscriber(Consumer<State> subscriber) {
        fadeTimeSubscribers.add(subscriber);
    }

    public void addLightStateSubscriber(Consumer<State> subscriber) {
        lightStateSubscribers.add(subscriber);
    }

    public void addLightLevelSubscriber(Consumer<State> subscriber) {
        lightLevelSubscribers.add(subscriber);
    }

    public void addActionFailedSubscriber(Runnable subscriber) {
        actionFailedSubscribers.add(subscriber);
    }

}
