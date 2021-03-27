package com.example.easymornings.preference;

public interface PreferencesConnector {
    String getString(String name, String defaultValue);
    void setString(String name, String value);
}
