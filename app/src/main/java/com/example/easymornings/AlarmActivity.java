package com.example.easymornings;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.easymornings.db.Alarm;
import com.example.easymornings.db.AlarmRepository;
import com.example.easymornings.light.LightConnector;
import com.example.easymornings.light.LightConnector.LightState;
import com.example.easymornings.light.LightConnector.LightStatus;
import com.example.easymornings.light.LightManager;

import static com.example.easymornings.AlarmScheduler.COMMAND_EXTRA;
import static com.example.easymornings.AlarmScheduler.SLEEP_SOUND_COMMAND;
import static com.example.easymornings.AlarmScheduler.SOUND_START_COMMAND;
import static com.example.easymornings.AlarmScheduler.UID_EXTRA;

public class AlarmActivity extends AppCompatActivity {

    Handler uiHandler;
    LightManager lightManager;
    SeekBar dimmerBar;
    ImageView onButton, offButton;
    Button dismissButton, sleepButton;
    MediaPlayer mediaPlayer;
    AlarmScheduler alarmScheduler;
    Alarm alarm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupWakeUp();
        setContentView(R.layout.activity_alarm);

        lightManager = new LightManager(LightConnector.Create(this));
        alarmScheduler = AlarmScheduler.create(getApplicationContext());

        setupLightUI();
        setupSubscribers();
        setupAlarmButtons();

        Bundle extras = getIntent().getExtras();
        handleAlarm(extras);
    }

    private void setupWakeUp() {
        setTurnScreenOn(true);
        setShowWhenLocked(true);
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        keyguardManager.requestDismissKeyguard(this, null);
    }

    private void setupAlarmButtons() {
        dismissButton = findViewById(R.id.dismiss);
        dismissButton.setOnClickListener(v -> dismissAlarm());
        sleepButton = findViewById(R.id.sleep);
        sleepButton.setOnClickListener(v -> onSleepClick());
    }

    private void setupSubscribers() {
        uiHandler = new Handler(Looper.myLooper());
        lightManager.addLightLevelSubscriber(this::updateSliderUiHandler);
        lightManager.addActionFailedSubscriber(this::notifyActionFailedUiHandler);
    }

    private void setupLightUI() {

        onButton = findViewById(R.id.onbutton);
        onButton.setOnClickListener(v -> lightManager.on());

        offButton = findViewById(R.id.offbutton);
        offButton.setOnClickListener(v -> lightManager.off());

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

    private void handleAlarm(Bundle extras) {
        if (!extras.containsKey(COMMAND_EXTRA) || !extras.containsKey(UID_EXTRA)) {
            NotificationUtils.displayProblemNotification(getApplicationContext(), "Could not play alarm", NotificationUtils.ALARM_SOUND_RECEIVER_PROBLEM);
        }
        int command = extras.getInt(COMMAND_EXTRA);
        if (command == SLEEP_SOUND_COMMAND || command == SOUND_START_COMMAND) {
            int uid = extras.getInt(UID_EXTRA);
            AlarmRepository alarmRepository = AlarmRepository.create(getApplicationContext());
            alarmRepository.getAlarm(uid).thenAccept(alarm -> {
                if (command == SOUND_START_COMMAND)
                    alarmScheduler.scheduleNextAlarm(alarm);
                this.alarm = alarm;
                soundAlarmUiHandler();
            });
        }
    }

    void soundAlarmUiHandler() {
        uiHandler.post(this::soundAlarm);
    }

    void soundAlarm() {
        if (mediaPlayer == null || !mediaPlayer.isPlaying()) {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            if (alarm.alarmSound != null) {
                mediaPlayer = MediaPlayer.create(this, Uri.parse(alarm.alarmSound), null, audioAttributes, audioManager.generateAudioSessionId());
            }

            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.alarm, audioAttributes, audioManager.generateAudioSessionId());
            }

            mediaPlayer.start();
            mediaPlayer.setLooping(true);
        }
    }

    @Override
    public void onBackPressed() {
        if (mediaPlayer != null && mediaPlayer.isPlaying())
            mediaPlayer.stop();
        super.onBackPressed();
    }

    private void dismissAlarm() {
        if (mediaPlayer != null && mediaPlayer.isPlaying())
            mediaPlayer.stop();
        alarmScheduler.cancelSleepAlarm(alarm);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void onSleepClick() {
        mediaPlayer.stop();
        sleepButton.setVisibility(View.GONE);
        int delay = alarmScheduler.scheduleSleepAlarm(alarm);
        String msg = String.format("Alarm will sound in %s", TimeUtils.getTimeIntervalString(delay));
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
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

    synchronized private void updateSlider(LightStatus state) {
        boolean visible = state.getLightState() != LightState.NOT_CONNECTED;
        int visibility = visible ? View.VISIBLE : View.INVISIBLE;
        dimmerBar.setVisibility(visibility);
        onButton.setVisibility(visibility);
        offButton.setVisibility(visibility);
        if (visible)
            dimmerBar.setProgress((int) (dimmerBar.getMax() * state.getLightLevel()), false);
    }

}