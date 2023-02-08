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

    @Override
    public boolean getBool(String name, boolean defaultValue) {
        return sharedPreferences.getBoolean(name, defaultValue);
    }

    @Override
    public void setBool(String name, boolean value) {
        sharedPreferences.edit().putBoolean(name, value).apply();
    }

    @Override
    public int getInt(String name, int defaultValue) {
        return sharedPreferences.getInt(name, defaultValue);
    }

    @Override
    public void setInt(String name, int value) {
        sharedPreferences.edit().putInt(name, value).apply();
    }
}
