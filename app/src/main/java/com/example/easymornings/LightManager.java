package com.example.easymornings;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.example.easymornings.LightConnector.LightState;

import lombok.Getter;

public class LightManager {

    final LightConnector lightConnector;
    @Getter
    LightState lightState;
    @Getter
    int fadeTime;

    LightManager(LightConnector lightConnector) {
        this.lightConnector = lightConnector;
        this.fadeTime = 0;
        this.lightState = LightState.UNDEFINED;
    }

    public CompletableFuture<Boolean> checkLightState() {
        return lightConnector.getLightState().thenApply(state -> {
            if (lightState != state)
                changeState(state);
            return lightState != state;
        });
    }

    void addFadeTime(int amount) {
        fadeTime += amount;
    }

    void changeState(LightState lightState) {
        this.lightState = lightState;
        this.fadeTime = 0;
    }

    CompletableFuture<Boolean> onNow() {
        return lightConnector.onNow().thenApply((success) -> {
            if (success)
                changeState(LightState.ON);
            return success;
        });
    }

    CompletableFuture<Boolean> onTimer(int period) {
        return lightConnector.onTimer(period).thenApply((success) -> {
            if (success)
                changeState(LightState.ON);
            return success;
        });
    }

    CompletableFuture<Boolean> fadeOnNow(int period) {
        return lightConnector.fadeOnNow(period).thenApply((success) -> {
            if (success)
                changeState(LightState.FADING_ON);
            return success;
        });
    }

    CompletableFuture<Boolean> offNow() {
        return lightConnector.offNow().thenApply((success) -> {
            if (success)
                changeState(LightState.OFF);
            return success;
        });
    }

    CompletableFuture<Boolean> fadeOffNow(int period) {
        return lightConnector.fadeOffNow(period).thenApply((success) -> {
            if (success)
                changeState(LightState.FADING_OFF);
            return success;
        });
    }

}
