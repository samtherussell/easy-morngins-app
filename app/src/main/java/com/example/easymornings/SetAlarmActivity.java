package com.example.easymornings;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.easymornings.db.Alarm;

public class SetAlarmActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 51;

    AlarmController alarmController;
    Handler uiHandler;
    AlarmArrayAdapter adapter;
    ListView alarmList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_alarm);

        ImageButton addButton = findViewById(R.id.add_new_alarm);
        addButton.setOnClickListener(this::addNewAlarm);

        alarmList = findViewById(R.id.alarmlist);

        uiHandler = new Handler();

        new Thread(() -> {
            alarmController = AlarmController.create(getApplicationContext());
            adapter = new AlarmArrayAdapter(this, alarmController);
            alarmController.loadAlarmsFromStore();
            uiHandler.post(() -> {
                alarmList.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                findViewById(R.id.alarmloadingprogress).setVisibility(View.INVISIBLE);
            });
        }).start();

    }

    void addNewAlarm(View view) {
        new Thread(() -> {
            alarmController.createNewAlarm();
            uiHandler.post(() -> adapter.notifyDataSetChanged());
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        int position = requestCode;
        if (resultCode == -1) {
            Alarm alarm = adapter.getItem(position);
            final Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                if (uri.toString().contains("external")) {
                    int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
                    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
                    }
                }

                Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), uri);
                String name = ringtone.getTitle(this);
                View alarmView = alarmList.getChildAt(position);
                TextView currentSound = alarmView.findViewById(R.id.currentsound);
                currentSound.setText(name);

                alarm.alarmSound = uri.toString();
                alarmController.onChangeAlarmSound(alarm);

                Toast.makeText(getApplicationContext(), "Alarm Sounds Changed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "Will not be able to play chosen sound", Toast.LENGTH_SHORT).show();
            }
        }
    }

}