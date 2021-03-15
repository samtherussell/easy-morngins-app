package com.example.easymornings;

import android.app.AlarmManager;
import android.app.KeyguardManager;
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
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.easymornings.LightConnector.LightState;

import static com.example.easymornings.TimeUtils.getFadeTimeString;

public class MainActivity extends AppCompatActivity {

    public static final String COMMAND_EXTRA = "command";
    public static final int SOUND_START_COMMAND = 2;
    public static final int SLEEP_SOUND_COMMAND = 4;
    public static final int ALARM_SLEEP_DELAY = 5 * 60;

    Handler uiHandler;
    LightManager lightManager;
    TextView switchHint;
    SeekBar dimmerBar;
    ImageView onButton, offButton;
    Button plus5sec, plus1min, plus5min, dismiss, sleep;
    MediaPlayer mediaPlayer;
    AlarmController alarmController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTurnScreenOn(true);
        setShowWhenLocked(true);
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        keyguardManager.requestDismissKeyguard(this, null);
        setShowWhenLocked(true);
        setContentView(R.layout.activity_main);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        SharedPreferences sharedPreferences = getSharedPreferences(AppPreferenceValues.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        PreferencesConnector preferencesConnector = new SharedPreferencesConnector(sharedPreferences);
        alarmController = new AlarmController(alarmManager, preferencesConnector, getApplicationContext());

        findViewById(R.id.settings).setOnClickListener((v) -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.clockTime).setOnClickListener((v) -> startActivity(new Intent(this, SetAlarmActivity.class)));

        switchHint = findViewById(R.id.switchhint);

        dimmerBar = findViewById(R.id.seekBar);
        dimmerBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float level = ((float) progress) / seekBar.getMax();
                    lightManager.setLevel(level);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        uiHandler = new Handler(Looper.myLooper());

        LightConnector lightConnector = new LightConnector(() ->
                preferencesConnector.getString(AppPreferenceValues.SHARED_PREFERENCES_IP_ADDRESS, ""));

        lightManager = new LightManager(lightConnector);

        lightManager.addFadeTimeSubscriber((s) -> uiHandler.post(() -> this.updateSwitchHint(s)));

        lightManager.addLightStateSubscriber((s) -> uiHandler.post(() -> {
            this.updateSwitchHint(s);
            this.updateTimeButtons(s);
            this.updateSlider(s);
        }));

        lightManager.addLightLevelSubscriber((s) -> uiHandler.post(() -> this.updateSlider(s)));

        lightManager.addActionFailedSubscriber(() -> uiHandler.post(this::notifyActionFailed));

        onButton = findViewById(R.id.onbutton);
        onButton.setOnClickListener(v -> lightManager.on());

        offButton = findViewById(R.id.offbutton);
        offButton.setOnClickListener(v -> lightManager.off());

        plus5sec = findViewById(R.id.plus5sec);
        plus5sec.setOnClickListener(v -> lightManager.addFadeTime(5));

        plus1min = findViewById(R.id.plus1min);
        plus1min.setOnClickListener(v -> lightManager.addFadeTime(60));

        plus5min = findViewById(R.id.plus5min);
        plus5min.setOnClickListener(v -> lightManager.addFadeTime(5*60));

        dismiss = findViewById(R.id.dismiss);
        dismiss.setVisibility(View.GONE);
        dismiss.setOnClickListener(v -> dismissAlarm());
        sleep = findViewById(R.id.sleep);
        sleep.setVisibility(View.GONE);
        sleep.setOnClickListener(v -> onSleepClick());

        Bundle extras = getIntent().getExtras();
        runCommand(extras);
    }

    private void runCommand(Bundle extras) {
        if (extras != null && extras.containsKey(COMMAND_EXTRA)) {

            int command = extras.getInt(COMMAND_EXTRA);
            switch (command) {
                case SOUND_START_COMMAND:
                    alarmController.scheduleNextAlarm();
                case SLEEP_SOUND_COMMAND:
                    soundAlarm();
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

            String uriString = alarmController.getAlarmSound();
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
        alarmController.cancelSleepAlarm();
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
        lightManager.checkLightState();
        uiHandler.postDelayed(this::checkLightState, 2000);
    }

    private void onSleepClick() {
        mediaPlayer.stop();
        sleep.setVisibility(View.GONE);
        int delay = alarmController.scheduleSleepAlarm(ALARM_SLEEP_DELAY);
        String msg = String.format("Alarm will sound in %s", TimeUtils.getTimeIntervalString(delay));
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    void notifyActionFailed() {
        Toast.makeText(getApplicationContext(), getString(R.string.action_failed), Toast.LENGTH_LONG).show();
    }

    synchronized private void updateTimeButtons(LightManager.State state) {
        LightState lightState = state.getLightState();
        boolean enabled = lightState != LightState.NOT_CONNECTED;
        plus5sec.setEnabled(enabled);
        plus1min.setEnabled(enabled);
        plus5min.setEnabled(enabled);
    }

    synchronized private void updateSlider(LightManager.State state) {
        dimmerBar.setProgress((int) (dimmerBar.getMax() * state.getLevel()), false);
    }

    synchronized void updateSwitchHint(LightManager.State state) {
        final String message;
        int fadeTime = state.getFadeTime();
        switch (state.getLightState()) {
            case CONSTANT:
                if (fadeTime == 0)
                    message = getString(R.string.instantly);
                else
                    message = String.format("%s %s", getFadeTimeString(fadeTime), getString(R.string.fade));
                break;
            case FADING:
                message = getString(R.string.fading);
                break;
           case NOT_CONNECTED:
                message = getString(R.string.notconnected);
                break;
            default:
                throw new RuntimeException();
        }
        switchHint.setText(message);
    }
}