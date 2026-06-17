package com.tuhoang.pocketmind.utils

import android.content.Context
import android.content.SharedPreferences

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PrefsManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)

    private val _currencyFlow = MutableStateFlow(DEFAULT_CURRENCY)

    init {
        _currencyFlow.value = getCurrency()
    }

    companion object {
        const val PREF_SETTINGS = "PREF_SETTINGS"
        const val PREF_THEME = "PREF_THEME"
        const val PREF_CURRENCY = "PREF_CURRENCY"
        const val PREF_CUSTOM_CATEGORIES = "PREF_CUSTOM_CATEGORIES"
        const val DEFAULT_CURRENCY = "VND"
        const val PREF_WORKER_URL = "PREF_WORKER_URL"
        const val PREF_OPENAI_API_KEY = "PREF_OPENAI_API_KEY"
        const val PREF_OPENAI_MODEL = "PREF_OPENAI_MODEL"
        const val PREF_AI_TRANSLATION_ENABLED = "PREF_AI_TRANSLATION_ENABLED"
        const val PREF_CAMERA_ENABLED = "PREF_CAMERA_ENABLED"
        const val PREF_STORAGE_ENABLED = "PREF_STORAGE_ENABLED"
        const val PREF_MIC_ENABLED = "PREF_MIC_ENABLED"
        const val PREF_ONBOARDING_COMPLETE = "PREF_ONBOARDING_COMPLETE"
        const val PREF_BIOMETRIC_ENABLED = "PREF_BIOMETRIC_ENABLED"
        const val PREF_MONTHLY_BUDGET = "PREF_MONTHLY_BUDGET"
        const val PREF_DYNAMIC_COLOR = "PREF_DYNAMIC_COLOR"
        const val PREF_HAPTIC_ENABLED = "PREF_HAPTIC_ENABLED"

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

    fun getCurrency(defaultCurrency: String = DEFAULT_CURRENCY): String =
        prefs.getString(PREF_CURRENCY, defaultCurrency) ?: defaultCurrency

    fun currencyFlow(): StateFlow<String> = _currencyFlow.asStateFlow()

    fun setCurrency(currency: String) {
        prefs.edit().putString(PREF_CURRENCY, currency).apply()
        _currencyFlow.value = currency
    }

    fun getCustomCategories(): List<String> {
        val raw = prefs.getString(PREF_CUSTOM_CATEGORIES, "") ?: ""
        return raw.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun setCustomCategories(categories: List<String>) {
        prefs.edit().putString(PREF_CUSTOM_CATEGORIES, categories.joinToString("|")).apply()
    }

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

    fun clearLegacyApiSecrets() {
        prefs.edit()
            .remove(PREF_OPENAI_API_KEY)
            .remove(PREF_OPENAI_MODEL)
            .apply()
    }

    @Deprecated("API keys are managed server-side on the Worker")
    fun getOpenAiApiKey(): String = ""

    @Deprecated("API keys are managed server-side on the Worker")
    fun setOpenAiApiKey(key: String) = Unit

    @Deprecated("API keys are managed server-side on the Worker")
    fun getOpenAiModel(defaultModel: String): String = defaultModel

    @Deprecated("API keys are managed server-side on the Worker")
    fun setOpenAiModel(model: String) = Unit

    fun isAiTranslationEnabled(): Boolean = prefs.getBoolean(PREF_AI_TRANSLATION_ENABLED, false)
    fun setAiTranslationEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(PREF_AI_TRANSLATION_ENABLED, enabled).apply()

    fun isOnboardingComplete(): Boolean = prefs.getBoolean(PREF_ONBOARDING_COMPLETE, false)
    fun setOnboardingComplete(complete: Boolean) =
        prefs.edit().putBoolean(PREF_ONBOARDING_COMPLETE, complete).apply()

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(PREF_BIOMETRIC_ENABLED, false)
    fun setBiometricEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(PREF_BIOMETRIC_ENABLED, enabled).apply()

    fun getMonthlyBudget(defaultBudget: Double = 0.0): Double =
        prefs.getFloat(PREF_MONTHLY_BUDGET, defaultBudget.toFloat()).toDouble()

    fun setMonthlyBudget(budget: Double) =
        prefs.edit().putFloat(PREF_MONTHLY_BUDGET, budget.toFloat()).apply()

    fun isDynamicColorEnabled(): Boolean = prefs.getBoolean(PREF_DYNAMIC_COLOR, true)
    fun setDynamicColorEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(PREF_DYNAMIC_COLOR, enabled).apply()

    fun isHapticEnabled(): Boolean = prefs.getBoolean(PREF_HAPTIC_ENABLED, true)
    fun setHapticEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(PREF_HAPTIC_ENABLED, enabled).apply()
}
