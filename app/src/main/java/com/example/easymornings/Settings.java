package com.example.easymornings;

import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

public class Settings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SharedPreferences sharedPreferences = AppPreferences.getSharePreferences(this);

        EditText text = findViewById(R.id.ipaddress);
        text.setText(sharedPreferences.getString(AppPreferences.SHARED_PREFERENCES_IP_ADDRESS, ""));

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
            sharedPreferences.edit().putString(AppPreferences.SHARED_PREFERENCES_IP_ADDRESS, ipAddress).apply();
            uiHandler.post(() -> Toast.makeText(getApplicationContext(), getString(R.string.saved), Toast.LENGTH_SHORT).show());
        });

    }
}