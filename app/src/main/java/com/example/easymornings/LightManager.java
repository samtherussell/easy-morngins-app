package com.example.easymornings;

import java.util.function.BiConsumer;
import com.example.easymornings.LightConnector.LightState;

public class LightManager {

    final LightConnector lightConnector;
    final BiConsumer<LightState, Integer> updateView;
    LightState lightState;
    int fadeTime;

    LightManager(LightConnector lightConnector, BiConsumer<LightState, Integer> updateView) {
        this.lightConnector = lightConnector;
        this.updateView = updateView;
        this.fadeTime = 0;
        checkLightState();

        updateView.accept(this.lightState, this.fadeTime);
    }

    public void checkLightState() {
        lightState = lightConnector.getLightState();
    }

    void addFadeTime(int amount) {
        fadeTime += amount;
        updateView.accept(lightState, fadeTime);
    }

    void reset() {
        fadeTime = 0;
        updateView.accept(lightState, fadeTime);
    }

    private void changeState(LightState lightState) {
        this.lightState = lightState;
        this.fadeTime = 0;
        this.updateView.accept(this.lightState, this.fadeTime);
    }

    void onNow() {
        lightConnector.onNow();
        changeState(LightState.ON);
    }

    void fadeOnNow(int period) {
        lightConnector.fadeOnNow(period);
        changeState(LightState.FADING_ON);
    }

    void offNow() {
        lightConnector.offNow();
        changeState(LightState.OFF);
    }

    void fadeOffNow(int period) {
        lightConnector.fadeOffNow(period);
        changeState(LightState.FADING_OFF);
    }

}
