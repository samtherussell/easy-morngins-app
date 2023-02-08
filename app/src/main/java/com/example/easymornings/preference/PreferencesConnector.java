package com.example.easymornings.preference;

public interface PreferencesConnector {
    String getString(String name, String defaultValue);
    void setString(String name, String value);
    boolean getBool(String name, boolean defaultValue);
    void setBool(String name, boolean value);
    int getInt(String name, int defaultValue);
    void setInt(String name, int value);
}
