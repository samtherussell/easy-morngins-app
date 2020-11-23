package com.example.easymornings;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.easymornings.LightConnector.LightState;

public class MainActivity extends AppCompatActivity {

    public static final String COMMAND_EXTRA = "command";
    public static final String FADE_IN_EXTRA = "fadeIn";
    public static final int FADE_ON_COMMAND = 1;
    public static final int SOUND_START_COMMAND = 2;
    public static final int ALL_OFF_COMMAND = 3;
    public static final int SLEEP_SOUND_COMMAND = 4;

    Handler uiHandler;
    LightManager lightManager;
    TextView switchHint;
    ImageView mainSwitch, onButton, offButton;
    Button plus5sec, plus1min, plus5min, dismiss, sleep;
    MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.settings).setOnClickListener((v) -> startActivity(new Intent(this, Settings.class)));
        findViewById(R.id.clockTime).setOnClickListener((v) -> startActivity(new Intent(this, SetAlarm.class)));

        switchHint = findViewById(R.id.switchhint);

        mainSwitch = findViewById(R.id.sliderbutton);
        mainSwitch.setOnClickListener(v -> onSwitchClick());

        onButton = findViewById(R.id.onbutton);
        onButton.setOnClickListener(v -> onNow());

        offButton = findViewById(R.id.offbutton);
        offButton.setOnClickListener(v -> offNow());

        plus5sec = findViewById(R.id.plus5sec);
        plus5sec.setOnClickListener(v -> addFadeTime(5));

        plus1min = findViewById(R.id.plus1min);
        plus1min.setOnClickListener(v -> addFadeTime(60));

        plus5min = findViewById(R.id.plus5min);
        plus5min.setOnClickListener(v -> addFadeTime(5*60));

        dismiss = findViewById(R.id.dismiss);
        dismiss.setVisibility(View.GONE);
        dismiss.setOnClickListener(v -> dismissAlarm());
        sleep = findViewById(R.id.sleep);
        sleep.setVisibility(View.GONE);
        sleep.setOnClickListener(v -> onSleepClick());

        uiHandler = new Handler(Looper.myLooper());

        LightConnector lightConnector = new LightConnector(this::getIPAddress);

        lightManager = new LightManager(lightConnector);

        Bundle extras = getIntent().getExtras();
        runCommand(extras);
    }

    private void runCommand(Bundle extras) {
        if (extras != null && extras.containsKey(COMMAND_EXTRA)) {

            int command = extras.getInt(COMMAND_EXTRA);
            switch (command) {
                case FADE_ON_COMMAND:
                    int time = extras.getInt(FADE_IN_EXTRA);
                    fadeOnNow(time);
                    SetAlarm.resetFadeInAlarm(this);
                    break;
                case SOUND_START_COMMAND:
                    SetAlarm.resetSoundAlarm(this);
                case SLEEP_SOUND_COMMAND:
                    soundAlarm();
                    break;
                case ALL_OFF_COMMAND:
                    offNow();
                    SetAlarm.resetOffAlarm(this);
                    break;
            }
        }

    }

    void soundAlarm() {
        if (mediaPlayer == null || !mediaPlayer.isPlaying()) {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            SharedPreferences sharedPreferences = AppPreferences.getSharePreferences(this);
            String uriString = sharedPreferences.getString(AppPreferences.SHARED_PREFERENCES_SOUND, null);
            if (uriString != null) {
                mediaPlayer = MediaPlayer.create(this, Uri.parse(uriString), null, audioAttributes, audioManager.generateAudioSessionId());
            }

            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.alarm, audioAttributes, audioManager.generateAudioSessionId());
            }

            mediaPlayer.start();
            mediaPlayer.setLooping(true);

            dismiss.setVisibility(View.VISIBLE);
            sleep.setVisibility(View.VISIBLE);
        }
    }

    void dismissAlarm() {
        if (mediaPlayer != null && mediaPlayer.isPlaying())
            mediaPlayer.stop();
        dismiss.setVisibility(View.GONE);
        sleep.setVisibility(View.GONE);
        cancelSleepAlarm();
    }

    void notifyActionFailed() {
        Toast.makeText(getApplicationContext(), getString(R.string.action_failed), Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        uiHandler.post(this::checkLightState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        uiHandler.removeCallbacksAndMessages(null);
    }

    public void checkLightState() {
        lightManager.checkLightState().thenAccept(r -> { if (r) uiHandler.post(this::updateUI); });
        uiHandler.postDelayed(this::checkLightState, 1000);
    }

    String getIPAddress() {
        SharedPreferences sharedPreferences = AppPreferences.getSharePreferences(this);
        return sharedPreferences.getString(AppPreferences.SHARED_PREFERENCES_IP_ADDRESS, "");
    }

    private void onSwitchClick() {
        if (lightManager.lightState == LightState.OFF) {
            if (lightManager.fadeTime == 0)
                onNow();
            else
                fadeOnNow(lightManager.fadeTime);
        } else if (lightManager.lightState == LightConnector.LightState.ON) {
            if (lightManager.fadeTime == 0)
                offNow();
            else
                fadeOffNow(lightManager.fadeTime);
        }
    }

    private void onSleepClick() {
        mediaPlayer.stop();
        sleep.setVisibility(View.GONE);
        scheduleSleepAlarm(60);
    }

    private void scheduleSleepAlarm(int seconds) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.COMMAND_EXTRA, MainActivity.SLEEP_SOUND_COMMAND);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, MainActivity.SLEEP_SOUND_COMMAND, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + seconds*1000, pendingIntent);
    }

    private void cancelSleepAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.COMMAND_EXTRA, MainActivity.SLEEP_SOUND_COMMAND);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, MainActivity.SLEEP_SOUND_COMMAND, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);
    }

    private void addFadeTime(int amount) {
        lightManager.addFadeTime(amount);
        updateUI();
    }

    private void onNow() {
        lightManager.onNow().thenAccept(this::updateOrNotify);
    }

    private void offNow() {
        lightManager.offNow().thenAccept(this::updateOrNotify);
    }

    private void fadeOnNow(int fadeTime) {
        lightManager.fadeOnNow(fadeTime).thenAccept(this::updateOrNotify);
    }

    private void fadeOffNow(int fadeTime) {
        lightManager.fadeOffNow(fadeTime).thenAccept(this::updateOrNotify);
    }

    void updateOrNotify(boolean success) {
        if (success)
            uiHandler.post(this::updateUI);
        else
            uiHandler.post(this::notifyActionFailed);
    }

    synchronized void updateUI() {
        LightState lightState = lightManager.getLightState();
        int fadeTime = lightManager.getFadeTime();
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
            case NOT_CONNECTED:
                message = getString(R.string.notconnected);
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
            return String.format("%d:%02d", minutes, seconds);
        else if (minutes > 0)
            return String.format("%d %s", minutes, getString(R.string.minute));
        else
            return String.format("%d %s", seconds, getString(R.string.second));
    }
}