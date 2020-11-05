package com.example.easymornings;


import android.os.Handler;
import android.os.Message;

import java.util.function.BiConsumer;

import com.example.easymornings.LightConnector.LightState;

public class LightManager {

    final LightConnector lightConnector;
    final BiConsumer<LightState, Integer> updateView;
    LightState lightState;
    int fadeTime;
    Handler uiUpdateHandler;

    LightManager(LightConnector lightConnector, BiConsumer<LightState, Integer> updateView, Handler uiUpdateHandler) {
        this.lightConnector = lightConnector;
        this.updateView = updateView;
        this.uiUpdateHandler = uiUpdateHandler;
        this.fadeTime = 0;
        this.lightState = LightState.OFF;
        updateView.accept(this.lightState, this.fadeTime);
    }

    public void checkLightState() {
        lightConnector.getLightState().thenAccept(state -> {
            if (lightState != state)
                Message.obtain(uiUpdateHandler, 1, state).sendToTarget();
        });
    }

    void addFadeTime(int amount) {
        fadeTime += amount;
        updateView.accept(lightState, fadeTime);
    }

    void changeState(LightState lightState) {
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
