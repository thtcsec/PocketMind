package com.tuhoang.pocketmind;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class PocketMindApp extends Application {

    public static final String PREF_SETTINGS = "PREF_SETTINGS";
    public static final String PREF_THEME = "PREF_THEME"; // 0: System, 1: Light, 2: Dark
    public static final String PREF_NOTIFICATIONS = "PREF_NOTIFICATIONS";
    public static final String PREF_CURRENCY = "PREF_CURRENCY";
    
    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    @Override
    public void onCreate() {
        super.onCreate();
        applyPersistedTheme();
    }

    private void applyPersistedTheme() {
        SharedPreferences prefs = getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE);
        int themePref = prefs.getInt(PREF_THEME, THEME_SYSTEM);
        
        switch (themePref) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
