package com.example.easymornings.preference;

import android.content.SharedPreferences;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SharedPreferencesConnector implements PreferencesConnector {

    final SharedPreferences sharedPreferences;

    @Override
    public String getString(String name, String defaultValue) {
        return sharedPreferences.getString(name, defaultValue);
    }

    @Override
    public void setString(String name, String value) {
        sharedPreferences.edit().putString(name, value).apply();
    }
}
