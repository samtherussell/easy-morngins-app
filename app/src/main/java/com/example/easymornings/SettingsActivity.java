package com.example.easymornings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SharedPreferences sharedPreferences = getSharedPreferences(AppPreferenceValues.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);

        EditText text = findViewById(R.id.ipaddress);
        text.setText(sharedPreferences.getString(AppPreferenceValues.SHARED_PREFERENCES_IP_ADDRESS, ""));

        Handler uiHandler = new Handler(getMainLooper());

        findViewById(R.id.check).setOnClickListener((v -> {
            String ipAddress = text.getText().toString();
            Toast.makeText(getApplicationContext(), getString(R.string.trying_to_connect), Toast.LENGTH_SHORT).show();
            new LightConnector(() -> ipAddress).getLightState().thenAccept(state -> {
                if (state == LightConnector.LightState.NOT_CONNECTED) {
                    uiHandler.post(() -> Toast.makeText(getApplicationContext(), getString(R.string.connection_failed), Toast.LENGTH_SHORT).show());
                } else {
                    uiHandler.post(() -> Toast.makeText(getApplicationContext(), getString(R.string.connection_successful), Toast.LENGTH_SHORT).show());
                }
            });
        }));

        findViewById(R.id.save).setOnClickListener(v -> {
            String ipAddress = text.getText().toString();
            sharedPreferences.edit().putString(AppPreferenceValues.SHARED_PREFERENCES_IP_ADDRESS, ipAddress).apply();
            uiHandler.post(() -> Toast.makeText(getApplicationContext(), getString(R.string.saved), Toast.LENGTH_SHORT).show());
        });

    }
}