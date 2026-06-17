package com.tuhoang.pocketmind

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.firestore.FirebaseFirestore
import com.tuhoang.pocketmind.utils.AppLogger
import com.tuhoang.pocketmind.utils.PrefsManager

class PocketMindApp : Application() {

    companion object {
        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2
        const val DEFAULT_WORKER_URL = "https://pocketmind.tht-csec2005.workers.dev"
    }

    override fun onCreate() {
        super.onCreate()
        PrefsManager.init(this)
        PrefsManager.getInstance().clearLegacyApiSecrets()
        if (PrefsManager.getInstance().getWorkerUrl().isBlank()) {
            PrefsManager.getInstance().setWorkerUrl(DEFAULT_WORKER_URL)
        }
        applyPersistedTheme()
        fetchGlobalConfig()
    }

    private fun fetchGlobalConfig() {
        try {
            FirebaseFirestore.getInstance()
                .collection("system_configs")
                .document("global")
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        document.getString("worker_url")?.takeIf { it.isNotEmpty() }?.let { url ->
                            PrefsManager.getInstance().setWorkerUrl(url)
                            AppLogger.d("PocketMindApp", "Synced Global Worker URL: $url")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    AppLogger.e("PocketMindApp", "Failed to fetch global config", e)
                }
        } catch (e: Exception) {
            AppLogger.e("PocketMindApp", "Firebase not yet initialized or error in global sync", e)
        }
    }

    private fun applyPersistedTheme() {
        when (PrefsManager.getInstance().getTheme(THEME_SYSTEM)) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}
