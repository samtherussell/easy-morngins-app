package com.example.easymornings;

import android.content.SharedPreferences;

import lombok.RequiredArgsConstructor;

interface PreferencesConnector {
    int getInt(String name, int defaultValue);
    void setInt(String name, int value);
    long getLong(String name, long defaultValue);
    void setLong(String name, long value);
    boolean getBoolean(String name, boolean defaultValue);
    void setBoolean(String name, boolean value);
    String getString(String name, String defaultValue);
    void setString(String name, String value);
}

@RequiredArgsConstructor
public class SharedPreferencesConnector implements PreferencesConnector {

    final SharedPreferences sharedPreferences;

    @Override
    public int getInt(String name, int defaultValue) {
        return sharedPreferences.getInt(name, defaultValue);
    }

    @Override
    public void setInt(String name, int value) {
        sharedPreferences.edit().putInt(name, value).commit();
    }

    @Override
    public long getLong(String name, long defaultValue) {
        return sharedPreferences.getLong(name, defaultValue);
    }

    @Override
    public void setLong(String name, long value) {
        sharedPreferences.edit().putLong(name, value).commit();
    }

    @Override
    public boolean getBoolean(String name, boolean defaultValue) {
        return sharedPreferences.getBoolean(name, defaultValue);
    }

    @Override
    public void setBoolean(String name, boolean value) {
        sharedPreferences.edit().putBoolean(name, value).commit();
    }

    @Override
    public String getString(String name, String defaultValue) {
        return sharedPreferences.getString(name, defaultValue);
    }

    @Override
    public void setString(String name, String value) {
        sharedPreferences.edit().putString(name, value).commit();
    }
}
