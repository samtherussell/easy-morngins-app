package com.example.easymornings;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.easymornings.light.LightConnector;
import com.example.easymornings.light.LightConnector.LightState;
import com.example.easymornings.light.LightConnector.LightStatus;
import com.example.easymornings.light.LightManager;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.example.easymornings.TimeUtils.getDelayTimeString;
import static com.example.easymornings.TimeUtils.getTimeLeftString;

public class MainActivity extends AppCompatActivity {

    Handler uiHandler;
    LightManager lightManager;
    TextView delayHint, statusHint;
    SeekBar dimmerBar;
    ImageView onButton, offButton;
    View delayButtons;
    Button plus15secButton, plus1minButton, plus5minButton;
    ImageButton cancelButton;
    Dialog pendingSpinner;
    final int PENDING_SPINNER_DELAY = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.settings).setOnClickListener((v) -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.clockTime).setOnClickListener((v) -> startActivity(new Intent(this, SetAlarmActivity.class)));

        lightManager = new LightManager(LightConnector.Create(this));
        lightManager.startCheckLightState();

        setupLightUI();
        setupDelayUI();
        setupSubscribers();
    }

    private void setupSubscribers() {
        uiHandler = new Handler(Looper.myLooper());
        lightManager.addLightStateSubscriber(this::updateStatusHintUiHandler);
        lightManager.addLightStateSubscriber(this::updateDelayButtonsUiHandler);
        lightManager.addLightStateSubscriber(this::setDelayHintVisibilityUiHandler);
        lightManager.addLightStateSubscriber(this::updateSliderUiHandler);
        lightManager.addLightLevelSubscriber(this::updateSliderUiHandler);
        lightManager.addDelayTimeSubscriber(this::updateDelayHintUiHandler);
        lightManager.addTimeLeftSubscribers(this::updateStatusHintUiHandler);
        lightManager.addActionFailedSubscriber(this::notifyActionFailedUiHandler);
    }

    private void setupLightUI() {
        statusHint = findViewById(R.id.statushint);
        statusHint.setText("trying to connect...");

        onButton = findViewById(R.id.onbutton);
        onButton.setVisibility(View.INVISIBLE);
        onButton.setOnClickListener(v -> on());

        offButton = findViewById(R.id.offbutton);
        offButton.setVisibility(View.INVISIBLE);
        offButton.setOnClickListener(v -> off());

        dimmerBar = findViewById(R.id.seekBar);
        dimmerBar.setVisibility(View.INVISIBLE);
        dimmerBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float level = ((float) progress) / seekBar.getMax();
                    setLevel(level);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

    }

    private void setupDelayUI() {
        delayHint = findViewById(R.id.delayhint);
        delayButtons = findViewById(R.id.delay_buttons);

        plus15secButton = findViewById(R.id.plus15sec);
        plus15secButton.setOnClickListener(v -> lightManager.addDelayTime(15));

        plus1minButton = findViewById(R.id.plus1min);
        plus1minButton.setOnClickListener(v -> lightManager.addDelayTime(60));

        plus5minButton = findViewById(R.id.plus5min);
        plus5minButton.setOnClickListener(v -> lightManager.addDelayTime(5 * 60));

        cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> lightManager.cancelDelayTime());

        setDelayButtonVisibility(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        lightManager.startCheckLightState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        lightManager.stopCheckLightState();
    }

    private void setDelayHintVisibilityUiHandler(LightStatus lightStatus) {
        uiHandler.post(() -> this.setDelayHintVisibility(lightStatus));
    }

    void updateDelayHintUiHandler(int delayTime) {
        uiHandler.post(() -> this.updateDelayHint(delayTime));
    }

    void updateStatusHintUiHandler(LightStatus state) {
        uiHandler.post(() -> this.updateStatusHint(state));
    }

    void updateDelayButtonsUiHandler(LightStatus state) {
        uiHandler.post(() -> this.updateDelayButtons(state));
    }

    void updateSliderUiHandler(LightStatus state) {
        uiHandler.post(() -> this.updateSlider(state));
    }

    void notifyActionFailedUiHandler() {
        uiHandler.post(this::notifyActionFailed);
    }

    void notifyActionFailed() {
        Toast.makeText(getApplicationContext(), getString(R.string.action_failed), Toast.LENGTH_LONG).show();
    }

    private void updateDelayButtons(LightStatus state) {
        boolean visible = state.getLightState() != LightState.NOT_CONNECTED;
        setDelayButtonVisibility(visible);
    }

    synchronized private void setDelayButtonVisibility(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.INVISIBLE;
        plus15secButton.setVisibility(visibility);
        plus1minButton.setVisibility(visibility);
        plus5minButton.setVisibility(visibility);
        cancelButton.setVisibility(visibility);
    }

    synchronized private void updateSlider(LightStatus state) {
        boolean visible = state.getLightState() != LightState.NOT_CONNECTED;
        int visibility = visible ? View.VISIBLE : View.INVISIBLE;
        dimmerBar.setVisibility(visibility);
        onButton.setVisibility(visibility);
        offButton.setVisibility(visibility);
        if (visible)
            dimmerBar.setProgress((int) (dimmerBar.getMax() * state.getLightLevel()), false);
    }

    synchronized private void updateDelayHint(int delayTime) {
        if (delayTime == -1)
            delayHint.setText("");
        else
            delayHint.setText(getDelayTimeString(delayTime));
    }

    synchronized private void setDelayHintVisibility(LightStatus lightStatus) {
        if (lightStatus.getLightState().equals(LightState.NOT_CONNECTED)) {
            delayHint.setVisibility(View.INVISIBLE);
        } else {
            delayHint.setVisibility(View.VISIBLE);
        }
    }

    synchronized void updateStatusHint(LightStatus state) {
        final String message;
        int timeLeft = state.getTimeLeft();
        switch (state.getLightState()) {
            case CONSTANT:
                message = "";
                break;
            case FADING:
                message = String.format("fading (%s)", getTimeLeftString(timeLeft));
                break;
            case TIMER:
                message = String.format("timer (%s)", getTimeLeftString(timeLeft));
                break;
            case NOT_CONNECTED:
                message = getString(R.string.notconnected);
                break;
            default:
                throw new RuntimeException();
        }
        statusHint.setText(message);
    }

    void on() {
        if (lightManager.hasDelayTime()) {
            openDelayPickerDialog(mode -> {
                lightManager.setDelayMode(mode);
                CompletableFuture<Void> runningRequest = lightManager.on().thenRun(() -> uiHandler.post(this::cancelSpinner));
                uiHandler.postDelayed(() -> this.showSpinner(runningRequest), PENDING_SPINNER_DELAY);
            });
        } else {
            CompletableFuture<Void> runningRequest = lightManager.on().thenRun(() -> uiHandler.post(this::cancelSpinner));
            uiHandler.postDelayed(() -> this.showSpinner(runningRequest), PENDING_SPINNER_DELAY);
        }
    }

    void off() {
        if (lightManager.hasDelayTime()) {
            openDelayPickerDialog(mode -> {
                lightManager.setDelayMode(mode);
                CompletableFuture<Void> runningRequest = lightManager.off().thenRun(() -> uiHandler.post(this::cancelSpinner));
                uiHandler.postDelayed(() -> this.showSpinner(runningRequest), PENDING_SPINNER_DELAY);
            });
        } else {
            CompletableFuture<Void> runningRequest = lightManager.off().thenRun(() -> uiHandler.post(this::cancelSpinner));
            uiHandler.postDelayed(() -> this.showSpinner(runningRequest), PENDING_SPINNER_DELAY);
        }
    }

    void setLevel(float level) {
        if (lightManager.hasDelayTime()) {
            openDelayPickerDialog(mode -> {
                lightManager.setDelayMode(mode);
                CompletableFuture<Void> runningRequest = lightManager.setLevel(level).thenRun(() -> uiHandler.post(this::cancelSpinner));
                uiHandler.postDelayed(() -> this.showSpinner(runningRequest), PENDING_SPINNER_DELAY);
            });
        } else{
            CompletableFuture<Void> runningRequest = lightManager.setLevel(level).thenRun(() -> uiHandler.post(this::cancelSpinner));
            uiHandler.postDelayed(() -> this.showSpinner(runningRequest), PENDING_SPINNER_DELAY);
        }
    }

    synchronized private void showSpinner(CompletableFuture<Void> runningRequest) {
        if (!runningRequest.isDone() && this.pendingSpinner == null) {
            final Dialog dialog = new Dialog(this);
            ProgressBar spinner = new ProgressBar(this, null, android.R.attr.progressBarStyleLarge);
            LinearLayout view = new LinearLayout(this);
            view.setOrientation(LinearLayout.VERTICAL);
            view.addView(spinner);
            Button cancel = new Button(this);
            cancel.setText("Cancel");
            cancel.setOnClickListener(v -> {
                dialog.dismiss();
            });
            view.addView(cancel);
            dialog.setContentView(view);
            dialog.getWindow().setGravity(Gravity.CENTER);
            dialog.setOnDismissListener(d -> {
                if (!runningRequest.isDone())
                    runningRequest.cancel(true);
                this.pendingSpinner = null;
            });
            dialog.setCancelable(false);
            dialog.show();
            this.pendingSpinner = dialog;
        }
    }

    private void cancelSpinner() {
        if (pendingSpinner != null) {
            pendingSpinner.dismiss();
            pendingSpinner = null;
        }
    }


    private void openDelayPickerDialog(Consumer<LightManager.DelayMode> onChange) {
        final Dialog dialog = new Dialog(this);
        dialog.setTitle("Delay Mode");
        dialog.setContentView(R.layout.delay_picker_dialog);
        Button fade = dialog.findViewById(R.id.dialog_fade);
        Button timer = dialog.findViewById(R.id.dialog_timer);
        fade.setOnClickListener(v -> {
            onChange.accept(LightManager.DelayMode.FADE);
            dialog.dismiss();
        });
        timer.setOnClickListener(v -> {
            onChange.accept(LightManager.DelayMode.TIMER);
            dialog.dismiss();
        });
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.getWindow().getAttributes().y = (int) (60 * getResources().getDisplayMetrics().scaledDensity);;
        dialog.show();
    }
}