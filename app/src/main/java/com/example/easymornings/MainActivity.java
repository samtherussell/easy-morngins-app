package com.example.easymornings;

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

import com.example.easymornings.light.LightConnector;
import com.example.easymornings.light.LightConnector.LightState;
import com.example.easymornings.db.Alarm;
import com.example.easymornings.db.AlarmRepository;
import com.example.easymornings.light.LightManager;
import com.example.easymornings.preference.AppPreferenceValues;
import com.example.easymornings.preference.PreferencesConnector;
import com.example.easymornings.preference.SharedPreferencesConnector;

import static com.example.easymornings.AlarmScheduler.COMMAND_EXTRA;
import static com.example.easymornings.AlarmScheduler.SLEEP_SOUND_COMMAND;
import static com.example.easymornings.AlarmScheduler.SOUND_START_COMMAND;
import static com.example.easymornings.AlarmScheduler.UID_EXTRA;
import static com.example.easymornings.TimeUtils.getFadeTimeString;

public class MainActivity extends AppCompatActivity {

    Handler uiHandler;
    LightManager lightManager;
    TextView switchHint;
    SeekBar dimmerBar;
    ImageView onButton, offButton;
    Button plus15sec, plus1min, plus5min, dismiss, sleep;
    MediaPlayer mediaPlayer;
    final int MAX_STATUS_CHECK_DELAY = 2000;
    final int MIN_STATUS_CHECK_DELAY = 100;
    int statusCheckDelay = MIN_STATUS_CHECK_DELAY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTurnScreenOn(true);
        setShowWhenLocked(true);
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        keyguardManager.requestDismissKeyguard(this, null);
        setShowWhenLocked(true);
        setContentView(R.layout.activity_main);

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

        SharedPreferences sharedPreferences = getSharedPreferences(AppPreferenceValues.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        PreferencesConnector preferencesConnector = new SharedPreferencesConnector(sharedPreferences);

        LightConnector lightConnector = new LightConnector(() ->
                preferencesConnector.getString(AppPreferenceValues.SHARED_PREFERENCES_IP_ADDRESS, ""));

        lightManager = new LightManager(lightConnector);

        lightManager.addFadeTimeSubscriber((s) -> uiHandler.post(() -> this.updateSwitchHint(s)));

        lightManager.addLightStateSubscriber((s) -> uiHandler.post(() -> {
            this.updateSwitchHint(s);
            this.updateTimeButtons(s);
            this.updateSlider(s);
            this.updateOnOffButtons(s);
        }));

        lightManager.addLightLevelSubscriber((s) -> uiHandler.post(() -> this.updateSlider(s)));

        lightManager.addActionFailedSubscriber(() -> uiHandler.post(this::notifyActionFailed));

        onButton = findViewById(R.id.onbutton);
        onButton.setOnClickListener(v -> lightManager.on());

        offButton = findViewById(R.id.offbutton);
        offButton.setOnClickListener(v -> lightManager.off());

        plus15sec = findViewById(R.id.plus15sec);
        plus15sec.setOnClickListener(v -> lightManager.addFadeTime(15));

        plus1min = findViewById(R.id.plus1min);
        plus1min.setOnClickListener(v -> lightManager.addFadeTime(60));

        plus5min = findViewById(R.id.plus5min);
        plus5min.setOnClickListener(v -> lightManager.addFadeTime(5*60));

        dismiss = findViewById(R.id.dismiss);
        dismiss.setVisibility(View.GONE);

        sleep = findViewById(R.id.sleep);
        sleep.setVisibility(View.GONE);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            handleAlarm(extras);
        }
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
        lightManager.checkLightState().thenAccept(changed -> {
            if (changed)
                statusCheckDelay = MIN_STATUS_CHECK_DELAY;
            else
                statusCheckDelay = Math.min(statusCheckDelay * 2, MAX_STATUS_CHECK_DELAY);
            onPause();
            uiHandler.postDelayed(this::checkLightState, statusCheckDelay);
        });
    }

    private void handleAlarm(Bundle extras) {
        if (!extras.containsKey(COMMAND_EXTRA) || !extras.containsKey(UID_EXTRA)) {
            NotificationUtils.displayProblemNotification(getApplicationContext(), "Could not play alarm", NotificationUtils.ALARM_SOUND_RECEIVER_PROBLEM);
        }
        int command = extras.getInt(COMMAND_EXTRA);
        if (command == SLEEP_SOUND_COMMAND || command == SOUND_START_COMMAND) {
            AlarmScheduler alarmScheduler = AlarmScheduler.create(getApplicationContext());
            int uid = extras.getInt(UID_EXTRA);
            AlarmRepository alarmRepository = AlarmRepository.create(getApplicationContext());
            alarmRepository.getAlarm(uid).thenAccept(alarm -> {
                if (command == SOUND_START_COMMAND)
                    alarmScheduler.scheduleNextAlarm(alarm);

                uiHandler.post(() -> {
                    soundAlarm(alarm.alarmSound);

                    dismiss.setVisibility(View.VISIBLE);
                    dismiss.setOnClickListener(v -> dismissAlarm(alarmScheduler, alarm));

                    sleep.setVisibility(View.VISIBLE);
                    sleep.setOnClickListener(v -> onSleepClick(alarmScheduler, alarm));
                });
            });
        }
    }

    void soundAlarm(String alarmSound) {
        if (mediaPlayer == null || !mediaPlayer.isPlaying()) {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            if (alarmSound != null) {
                mediaPlayer = MediaPlayer.create(this, Uri.parse(alarmSound), null, audioAttributes, audioManager.generateAudioSessionId());
            }

            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.alarm, audioAttributes, audioManager.generateAudioSessionId());
            }

            mediaPlayer.start();
            mediaPlayer.setLooping(true);
        }
    }

    private void dismissAlarm(AlarmScheduler alarmScheduler, Alarm alarm) {
        if (mediaPlayer != null && mediaPlayer.isPlaying())
            mediaPlayer.stop();
        dismiss.setVisibility(View.GONE);
        sleep.setVisibility(View.GONE);
        alarmScheduler.cancelSleepAlarm(alarm);
    }

    private void onSleepClick(AlarmScheduler alarmScheduler, Alarm alarm) {
        mediaPlayer.stop();
        sleep.setVisibility(View.GONE);
        int delay = alarmScheduler.scheduleSleepAlarm(alarm);
        String msg = String.format("Alarm will sound in %s", TimeUtils.getTimeIntervalString(delay));
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    void notifyActionFailed() {
        Toast.makeText(getApplicationContext(), getString(R.string.action_failed), Toast.LENGTH_LONG).show();
    }

    synchronized private void updateTimeButtons(LightManager.State state) {
        boolean enabled = state.getLightState() != LightState.NOT_CONNECTED;
        int visibility = enabled ? View.VISIBLE : View.INVISIBLE;
        plus15sec.setVisibility(visibility);
        plus1min.setVisibility(visibility);
        plus5min.setVisibility(visibility);
    }

    synchronized private void updateSlider(LightManager.State state) {
        boolean enabled = state.getLightState() != LightState.NOT_CONNECTED;
        int visibility = enabled ? View.VISIBLE : View.INVISIBLE;
        dimmerBar.setVisibility(visibility);

        dimmerBar.setProgress((int) (dimmerBar.getMax() * state.getLevel()), false);
    }

    private void updateOnOffButtons(LightManager.State state) {
        boolean enabled = state.getLightState() != LightState.NOT_CONNECTED;
        int visibility = enabled ? View.VISIBLE : View.INVISIBLE;
        onButton.setVisibility(visibility);
        offButton.setVisibility(visibility);
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