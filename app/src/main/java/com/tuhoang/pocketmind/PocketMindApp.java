package com.tuhoang.pocketmind;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tuhoang.pocketmind.utils.AppLogger;
import com.tuhoang.pocketmind.utils.PrefsManager;

public class PocketMindApp extends Application {

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    @Override
    public void onCreate() {
        super.onCreate();
        PrefsManager.init(this);
        applyPersistedTheme();
        fetchGlobalConfig();
    }

    private void fetchGlobalConfig() {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("system_configs").document("global").get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String workerUrl = documentSnapshot.getString("worker_url");
                        if (workerUrl != null && !workerUrl.isEmpty()) {
                            PrefsManager.getInstance().setWorkerUrl(workerUrl);
                            AppLogger.d("PocketMindApp", "Synced Global Worker URL: " + workerUrl);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    AppLogger.e("PocketMindApp", "Failed to fetch global config", e);
                });
        } catch (Exception e) {
            AppLogger.e("PocketMindApp", "Firebase not yet initialized or error in global sync", e);
        }
    }

    private void applyPersistedTheme() {
        int themePref = PrefsManager.getInstance().getTheme(THEME_SYSTEM);
        
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
