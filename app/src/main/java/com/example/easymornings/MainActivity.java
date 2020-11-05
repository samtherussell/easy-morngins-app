package com.example.easymornings;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.easymornings.LightConnector.LightState;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    ScheduledExecutorService executor;
    ScheduledFuture<?> fetchStatusTask;
    LightManager lightManager;
    TextView switchHint;
    ImageView mainSwitch, onButton, offButton;
    Button plus5sec, plus1min, plus5min;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        switchHint = findViewById(R.id.switchhint);

        mainSwitch = findViewById(R.id.sliderbutton);
        mainSwitch.setOnClickListener(this);

        onButton = findViewById(R.id.onbutton);
        onButton.setOnClickListener(this);

        offButton = findViewById(R.id.offbutton);
        offButton.setOnClickListener(this);

        plus5sec = findViewById(R.id.plus5sec);
        plus5sec.setOnClickListener(this);

        plus1min = findViewById(R.id.plus1min);
        plus1min.setOnClickListener(this);

        plus5min = findViewById(R.id.plus5min);
        plus5min.setOnClickListener(this);

        Handler uiUpdateHandler = new Handler(message -> {
            lightManager.changeState((LightState) message.obj);
            return true;
        });

        LightConnector lightConnector = new LightConnector("10.0.2.2", 8080);

        lightManager = new LightManager(lightConnector, this::update, uiUpdateHandler);

        executor = Executors.newSingleThreadScheduledExecutor();
        scheduleFetchStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fetchStatusTask != null)
            fetchStatusTask.cancel(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (fetchStatusTask == null || fetchStatusTask.isCancelled())
            scheduleFetchStatus();
    }

    private void scheduleFetchStatus() {
        fetchStatusTask = executor.scheduleAtFixedRate(lightManager::checkLightState, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.sliderbutton) {
            if (lightManager.lightState == LightState.OFF) {
                if (lightManager.fadeTime == 0)
                    lightManager.onNow();
                else
                    lightManager.fadeOnNow(lightManager.fadeTime);
            } else if (lightManager.lightState == LightConnector.LightState.ON) {
                if (lightManager.fadeTime == 0)
                    lightManager.offNow();
                else
                    lightManager.fadeOffNow(lightManager.fadeTime);
            }
        } else if (id == R.id.onbutton) {
            lightManager.onNow();
        } else if (id == R.id.offbutton) {
            lightManager.offNow();
        } if (id == R.id.plus5sec) {
            lightManager.addFadeTime(5);
        } else if (id == R.id.plus1min) {
            lightManager.addFadeTime(60);
        } else if(id == R.id.plus5min) {
            lightManager.addFadeTime(5 * 60);
        }
    }

    synchronized void update(LightState lightState, Integer fadeTime) {
        System.out.println("update");
        updateSlider(lightState);
        updateSwitchHint(lightState, fadeTime);
        updateTimeButtons(lightState);
    }

    private void updateTimeButtons(LightState lightState) {
        boolean enabled = lightState == LightState.OFF || lightState == LightState.ON;
        plus5sec.setEnabled(enabled);
        plus1min.setEnabled(enabled);
        plus5min.setEnabled(enabled);
    }

    private void updateSlider(LightState lightState) {
        switch (lightState) {
            case OFF:
                updateMainButtonPosition(0);
                mainSwitch.setImageResource(R.drawable.ic_switchbuttonoff);
                offButton.setVisibility(View.GONE);
                onButton.setVisibility(View.GONE);
                break;
            case ON:
                updateMainButtonPosition(125);
                mainSwitch.setImageResource(R.drawable.ic_switchbuttonon);
                offButton.setVisibility(View.GONE);
                onButton.setVisibility(View.GONE);
                break;
            case FADING_ON:
            case FADING_OFF:
                updateMainButtonPosition(62);
                mainSwitch.setImageResource(R.drawable.ic_switchbuttonoff);
                offButton.setVisibility(View.VISIBLE);
                onButton.setVisibility(View.VISIBLE);
        }
    }

    private void updateMainButtonPosition(int sp) {
        ConstraintLayout.LayoutParams mainSwitch = ((ConstraintLayout.LayoutParams) this.mainSwitch.getLayoutParams());
        int px = spToPx(sp);
        System.out.println(String.format("%d %d %d", sp, px, mainSwitch.getMarginStart()));
        if (mainSwitch.getMarginStart() != px) {
            mainSwitch.setMarginStart(px);
            this.mainSwitch.setLayoutParams(mainSwitch);
        }
    }

    public int spToPx(float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }

    void updateSwitchHint(LightState lightState, Integer fadeTime) {
        final String message;
        switch (lightState) {
            case OFF:
                if (fadeTime == 0)
                    message = getString(R.string.instant_on);
                else {
                    message = String.format("%s %s", getTimeString(fadeTime), getString(R.string.fade_on));
                }
                break;
            case ON:
                if (fadeTime == 0)
                    message = getString(R.string.instant_off);
                else {
                    message = String.format("%s %s", getTimeString(fadeTime), getString(R.string.fade_off));
                }
                break;
            case FADING_OFF:
                message = getString(R.string.fading_off);
                break;
            case FADING_ON:
                message = getString(R.string.fading_on);
                break;
            default:
                message = "bad light state";
        }
        switchHint.setText(message);
    }

    String getTimeString(int seconds) {
        int minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes > 0 && seconds > 0)
            return String.format("%d:%d", minutes, seconds);
        else if (minutes > 0)
            return String.format("%d %s", minutes, getString(R.string.minute));
        else
            return String.format("%d %s", seconds, getString(R.string.second));
    }

}