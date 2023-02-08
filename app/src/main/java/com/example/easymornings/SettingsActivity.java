package com.example.easymornings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.easymornings.light.LightConnector;
import com.example.easymornings.preference.AppPreferenceValues;
import com.example.easymornings.preference.PreferencesConnector;
import com.example.easymornings.preference.SharedPreferencesConnector;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SharedPreferences sharedPreferences = getSharedPreferences(AppPreferenceValues.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        PreferencesConnector preferencesConnector = new SharedPreferencesConnector(sharedPreferences);

        EditText ipAddr = findViewById(R.id.ipaddress);
        ipAddr.setText(preferencesConnector.getString(AppPreferenceValues.SHARED_PREFERENCES_IP_ADDRESS, ""));

        Switch autoRefreshSwitch = findViewById(R.id.auto_refresh);
        autoRefreshSwitch.setChecked(preferencesConnector.getBool(AppPreferenceValues.SHARED_PREFERENCES_AUTO_REFRESH, true));

        EditText autoRefreshTime = findViewById(R.id.auto_refresh_time);
        autoRefreshTime.setText(Integer.toString(preferencesConnector.getInt(AppPreferenceValues.SHARED_PREFERENCES_AUTO_REFRESH_TIME, 100)));

        Handler uiHandler = new Handler(getMainLooper());

        findViewById(R.id.check).setOnClickListener((v -> {
            String ipAddress = ipAddr.getText().toString();
            Toast.makeText(getApplicationContext(), getString(R.string.trying_to_connect), Toast.LENGTH_SHORT).show();
            new LightConnector(() -> ipAddress).getLightStatus().thenAccept(status -> {
                if (status.getLightState() == LightConnector.LightState.NOT_CONNECTED) {
                    uiHandler.post(() -> Toast.makeText(getApplicationContext(), getString(R.string.connection_failed), Toast.LENGTH_SHORT).show());
                } else {
                    uiHandler.post(() -> Toast.makeText(getApplicationContext(), getString(R.string.connection_successful), Toast.LENGTH_SHORT).show());
                }
            });
        }));

        findViewById(R.id.save).setOnClickListener(v -> {
            String ipAddress = ipAddr.getText().toString();
            preferencesConnector.setString(AppPreferenceValues.SHARED_PREFERENCES_IP_ADDRESS, ipAddress);

            preferencesConnector.setInt(AppPreferenceValues.SHARED_PREFERENCES_AUTO_REFRESH_TIME, Integer.parseInt(autoRefreshTime.getText().toString()));

            uiHandler.post(() -> Toast.makeText(getApplicationContext(), getString(R.string.saved), Toast.LENGTH_SHORT).show());
        });

        autoRefreshSwitch.setOnCheckedChangeListener((b, checked) -> {
            preferencesConnector.setBool(AppPreferenceValues.SHARED_PREFERENCES_AUTO_REFRESH, checked);
        });

    }
}