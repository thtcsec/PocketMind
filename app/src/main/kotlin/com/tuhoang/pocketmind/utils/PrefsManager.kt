package com.tuhoang.pocketmind.utils

import android.content.Context
import android.content.SharedPreferences

class PrefsManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)

    companion object {
        const val PREF_SETTINGS = "PREF_SETTINGS"
        const val PREF_THEME = "PREF_THEME"
        const val PREF_CURRENCY = "PREF_CURRENCY"
        const val PREF_WORKER_URL = "PREF_WORKER_URL"
        const val PREF_OPENAI_API_KEY = "PREF_OPENAI_API_KEY"
        const val PREF_OPENAI_MODEL = "PREF_OPENAI_MODEL"
        const val PREF_AI_TRANSLATION_ENABLED = "PREF_AI_TRANSLATION_ENABLED"
        const val PREF_CAMERA_ENABLED = "PREF_CAMERA_ENABLED"
        const val PREF_STORAGE_ENABLED = "PREF_STORAGE_ENABLED"
        const val PREF_MIC_ENABLED = "PREF_MIC_ENABLED"

        @Volatile
        private var instance: PrefsManager? = null

        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = PrefsManager(context)
                    }
                }
            }
        }

        fun getInstance(): PrefsManager =
            instance ?: error("PrefsManager not initialized. Call PrefsManager.init() in Application.onCreate()")
    }

    fun getTheme(defaultTheme: Int): Int = prefs.getInt(PREF_THEME, defaultTheme)
    fun setTheme(themeMode: Int) = prefs.edit().putInt(PREF_THEME, themeMode).apply()

    fun getCurrency(defaultCurrency: String): String =
        prefs.getString(PREF_CURRENCY, defaultCurrency) ?: defaultCurrency

    fun setCurrency(currency: String) = prefs.edit().putString(PREF_CURRENCY, currency).apply()

    fun getWorkerUrl(): String = prefs.getString(PREF_WORKER_URL, "") ?: ""
    fun setWorkerUrl(url: String) = prefs.edit().putString(PREF_WORKER_URL, url).apply()

    fun isCameraEnabled(defaultValue: Boolean): Boolean =
        prefs.getBoolean(PREF_CAMERA_ENABLED, defaultValue)

    fun setCameraEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(PREF_CAMERA_ENABLED, enabled).apply()

    fun isStorageEnabled(defaultValue: Boolean): Boolean =
        prefs.getBoolean(PREF_STORAGE_ENABLED, defaultValue)

    fun setStorageEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(PREF_STORAGE_ENABLED, enabled).apply()

    fun isMicEnabled(defaultValue: Boolean): Boolean =
        prefs.getBoolean(PREF_MIC_ENABLED, defaultValue)

    fun setMicEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(PREF_MIC_ENABLED, enabled).apply()

    fun getOpenAiApiKey(): String = prefs.getString(PREF_OPENAI_API_KEY, "") ?: ""
    fun setOpenAiApiKey(key: String) = prefs.edit().putString(PREF_OPENAI_API_KEY, key).apply()

    fun getOpenAiModel(defaultModel: String): String =
        prefs.getString(PREF_OPENAI_MODEL, defaultModel) ?: defaultModel

    fun setOpenAiModel(model: String) = prefs.edit().putString(PREF_OPENAI_MODEL, model).apply()

    fun isAiTranslationEnabled(): Boolean = prefs.getBoolean(PREF_AI_TRANSLATION_ENABLED, false)
    fun setAiTranslationEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(PREF_AI_TRANSLATION_ENABLED, enabled).apply()
}
