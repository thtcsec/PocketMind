package com.tuhoang.pocketmind.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public class PrefsManager {

    private static volatile PrefsManager instance;
    private final SharedPreferences prefs;

    public static final String PREF_SETTINGS = "PREF_SETTINGS";
    public static final String PREF_THEME = "PREF_THEME";
    public static final String PREF_NOTIFICATIONS = "PREF_NOTIFICATIONS";
    public static final String PREF_CURRENCY = "PREF_CURRENCY";
    public static final String PREF_WORKER_URL = "PREF_WORKER_URL";
    public static final String PREF_OPENAI_API_KEY = "PREF_OPENAI_API_KEY";
    public static final String PREF_OPENAI_MODEL = "PREF_OPENAI_MODEL";
    public static final String PREF_AI_TRANSLATION_ENABLED = "PREF_AI_TRANSLATION_ENABLED";

    public static final String PREF_CAMERA_ENABLED = "PREF_CAMERA_ENABLED";
    public static final String PREF_STORAGE_ENABLED = "PREF_STORAGE_ENABLED";
    public static final String PREF_MIC_ENABLED = "PREF_MIC_ENABLED";

    private PrefsManager(@NonNull Context context) {
        prefs = context
                .getApplicationContext()
                .getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE);
    }

    /**
     * Init once inside Application class
     */
    public static void init(@NonNull Context context) {
        if (instance == null) {
            synchronized (PrefsManager.class) {
                if (instance == null) {
                    instance = new PrefsManager(context);
                }
            }
        }
    }

    public static PrefsManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                    "PrefsManager not initialized. Call PrefsManager.init() in Application.onCreate()");
        }
        return instance;
    }

    // =====================
    // THEME
    // =====================

    public int getTheme(int defaultTheme) {
        return prefs.getInt(PREF_THEME, defaultTheme);
    }

    public void setTheme(int themeMode) {
        prefs.edit()
                .putInt(PREF_THEME, themeMode)
                .apply();
    }

    // =====================
    // CURRENCY
    // =====================

    public String getCurrency(String defaultCurrency) {
        return prefs.getString(PREF_CURRENCY, defaultCurrency);
    }

    public void setCurrency(String currency) {
        prefs.edit()
                .putString(PREF_CURRENCY, currency)
                .apply();
    }

    // =====================
    // WORKER URL
    // =====================

    public String getWorkerUrl() {
        return prefs.getString(PREF_WORKER_URL, "");
    }

    public void setWorkerUrl(String url) {
        prefs.edit()
                .putString(PREF_WORKER_URL, url)
                .apply();
    }

    // =====================
    // PERMISSIONS FLAGS
    // =====================

    public boolean isCameraEnabled(boolean defaultValue) {
        return prefs.getBoolean(PREF_CAMERA_ENABLED, defaultValue);
    }

    public void setCameraEnabled(boolean enabled) {
        prefs.edit()
                .putBoolean(PREF_CAMERA_ENABLED, enabled)
                .apply();
    }

    public boolean isStorageEnabled(boolean defaultValue) {
        return prefs.getBoolean(PREF_STORAGE_ENABLED, defaultValue);
    }

    public void setStorageEnabled(boolean enabled) {
        prefs.edit()
                .putBoolean(PREF_STORAGE_ENABLED, enabled)
                .apply();
    }

    public boolean isMicEnabled(boolean defaultValue) {
        return prefs.getBoolean(PREF_MIC_ENABLED, defaultValue);
    }

    public void setMicEnabled(boolean enabled) {
        prefs.edit()
                .putBoolean(PREF_MIC_ENABLED, enabled)
                .apply();
    }

    // =====================
    // OPENAI
    // =====================

    public String getOpenAiApiKey() {
        return prefs.getString(PREF_OPENAI_API_KEY, "");
    }

    public void setOpenAiApiKey(String key) {
        prefs.edit()
                .putString(PREF_OPENAI_API_KEY, key)
                .apply();
    }

    public String getOpenAiModel(String defaultModel) {
        return prefs.getString(PREF_OPENAI_MODEL, defaultModel);
    }

    public void setOpenAiModel(String model) {
        prefs.edit()
                .putString(PREF_OPENAI_MODEL, model)
                .apply();
    }

    // =====================
    // AI TRANSLATION
    // =====================

    public boolean isAiTranslationEnabled() {
        return prefs.getBoolean(PREF_AI_TRANSLATION_ENABLED, false);
    }

    public void setAiTranslationEnabled(boolean enabled) {
        prefs.edit()
                .putBoolean(PREF_AI_TRANSLATION_ENABLED, enabled)
                .apply();
    }

}