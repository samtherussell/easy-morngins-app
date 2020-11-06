package com.example.easymornings;


import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

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
                Message.obtain(uiUpdateHandler, MainActivity.CHANGE_STATE, state).sendToTarget();
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
        lightConnector.onNow().thenAccept((success) -> {
            if (success)
                Message.obtain(uiUpdateHandler, MainActivity.CHANGE_STATE, LightState.ON).sendToTarget();
            else
                Message.obtain(uiUpdateHandler, MainActivity.CONNECTION_FAILURE).sendToTarget();
        });
    }

    void onTimer(int period) {
        lightConnector.onTimer(period).thenAccept((success) -> {
            if (success)
                Message.obtain(uiUpdateHandler, MainActivity.CHANGE_STATE, LightState.ON).sendToTarget();
            else
                Message.obtain(uiUpdateHandler, MainActivity.CONNECTION_FAILURE).sendToTarget();
        });
    }

    void fadeOnNow(int period) {
        lightConnector.fadeOnNow(period).thenAccept((success) -> {
            if (success)
                Message.obtain(uiUpdateHandler, MainActivity.CHANGE_STATE, LightState.FADING_ON).sendToTarget();
            else
                Message.obtain(uiUpdateHandler, MainActivity.CONNECTION_FAILURE).sendToTarget();
        });
    }

    void offNow() {
        lightConnector.offNow().thenAccept((success) -> {
            if (success)
                Message.obtain(uiUpdateHandler, MainActivity.CHANGE_STATE, LightState.OFF).sendToTarget();
            else
                Message.obtain(uiUpdateHandler, MainActivity.CONNECTION_FAILURE).sendToTarget();
        });
    }

    void fadeOffNow(int period) {
        lightConnector.fadeOffNow(period).thenAccept((success) -> {
            if (success)
                Message.obtain(uiUpdateHandler, MainActivity.CHANGE_STATE, LightState.FADING_OFF).sendToTarget();
            else
                Message.obtain(uiUpdateHandler, MainActivity.CONNECTION_FAILURE).sendToTarget();
        });
    }

}
