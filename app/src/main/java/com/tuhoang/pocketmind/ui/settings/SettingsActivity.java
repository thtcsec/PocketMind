package com.tuhoang.pocketmind.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.os.LocaleListCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.tuhoang.pocketmind.PocketMindApp;
import com.tuhoang.pocketmind.R;
import com.tuhoang.pocketmind.databinding.ActivitySettingsBinding;
import com.tuhoang.pocketmind.utils.AppLogger;
import java.io.File;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Toolbar
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        prefs = getSharedPreferences(PocketMindApp.PREF_SETTINGS, Context.MODE_PRIVATE);

        // Setup Theme selection
        updateThemeText(prefs.getInt(PocketMindApp.PREF_THEME, PocketMindApp.THEME_SYSTEM));
        
        binding.llTheme.setOnClickListener(v -> {
            showThemeDialog();
        });

        // Setup Language display text based on current LocaleListCompat
        LocaleListCompat currentAppLocales = AppCompatDelegate.getApplicationLocales();
        if (!currentAppLocales.isEmpty() && currentAppLocales.get(0).getLanguage().equals("vi")) {
            binding.tvLanguageStatus.setText("Tiếng Việt");
        } else {
            binding.tvLanguageStatus.setText("English");
        }

        binding.llLanguage.setOnClickListener(v -> {
            showLanguageDialog();
        });

        // Setup Currency selection
        updateCurrencyText(prefs.getString(PocketMindApp.PREF_CURRENCY, "USD"));
        
        binding.llCurrency.setOnClickListener(v -> {
            showCurrencyDialog();
        });

        // Setup AI Configuration Routing
        binding.llAiConfig.setOnClickListener(v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                startActivity(new Intent(SettingsActivity.this, AiSettingsActivity.class));
            } else {
                Toast.makeText(this, "Bạn cần đăng nhập để thiết lập AI.", Toast.LENGTH_SHORT).show();
            }
        });

        binding.llClearCache.setOnClickListener(v -> {
            clearAppCache();
        });

        updateCacheSizeText();

        // Setup Notifications Switch
        binding.switchNotifications.setChecked(prefs.getBoolean(PocketMindApp.PREF_NOTIFICATIONS, true));
        binding.switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(PocketMindApp.PREF_NOTIFICATIONS, isChecked).apply();
            AppLogger.d("Notifications toggled: " + isChecked);
        });

        // Set dynamic app version
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            binding.tvAbout.setText("Version: " + version);
        } catch (PackageManager.NameNotFoundException e) {
            binding.tvAbout.setText("Version: Unknown");
            AppLogger.e("SettingsActivity", "Could not get app version", e);
        }
    }

    private void updateThemeText(int themePref) {
        if (themePref == PocketMindApp.THEME_LIGHT) {
            binding.tvThemeStatus.setText(getString(R.string.theme_light));
        } else if (themePref == PocketMindApp.THEME_DARK) {
            binding.tvThemeStatus.setText(getString(R.string.theme_dark));
        } else {
            binding.tvThemeStatus.setText(getString(R.string.theme_system));
        }
    }

    private void showThemeDialog() {
        String[] themes = {
            getString(R.string.theme_system), 
            getString(R.string.theme_light), 
            getString(R.string.theme_dark)
        };
        int currentTheme = prefs.getInt(PocketMindApp.PREF_THEME, PocketMindApp.THEME_SYSTEM);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.settings_theme))
                .setSingleChoiceItems(themes, currentTheme, (dialog, which) -> {
                    prefs.edit().putInt(PocketMindApp.PREF_THEME, which).apply();
                    
                    switch (which) {
                        case PocketMindApp.THEME_LIGHT:
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                            break;
                        case PocketMindApp.THEME_DARK:
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                            break;
                        case PocketMindApp.THEME_SYSTEM:
                        default:
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                            break;
                    }
                    
                    updateThemeText(which);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLanguageDialog() {
        String[] languages = {"English", "Tiếng Việt"};
        LocaleListCompat currentAppLocales = AppCompatDelegate.getApplicationLocales();
        int checkedItem = 0; // Default to English

        if (!currentAppLocales.isEmpty() && currentAppLocales.get(0).getLanguage().equals("vi")) {
            checkedItem = 1;
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Language")
                .setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
                    String selectedLanguageTag = (which == 0) ? "en" : "vi";
                    LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(selectedLanguageTag);
                    if (!appLocale.equals(AppCompatDelegate.getApplicationLocales())) {
                        AppCompatDelegate.setApplicationLocales(appLocale);
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateCurrencyText(String currencyCode) {
        binding.tvCurrencyStatus.setText(currencyCode);
    }

    private void showCurrencyDialog() {
        String[] currencies = {"USD", "AUD", "JPY", "VND"};
        String currentCurrency = prefs.getString(PocketMindApp.PREF_CURRENCY, "USD");
        int checkedItem = 0;
        for (int i = 0; i < currencies.length; i++) {
            if (currencies[i].equals(currentCurrency)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.settings_currency))
                .setSingleChoiceItems(currencies, checkedItem, (dialog, which) -> {
                    String selected = currencies[which];
                    prefs.edit().putString(PocketMindApp.PREF_CURRENCY, selected).apply();
                    updateCurrencyText(selected);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateCacheSizeText() {
        long size = getDirSize(getCacheDir());
        binding.tvClearCacheStatus.setText(formatSize(size));
    }

    private long getDirSize(File dir) {
        long size = 0;
        if (dir != null && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file != null && file.isFile()) {
                        size += file.length();
                    } else {
                        size += getDirSize(file);
                    }
                }
            }
        } else if (dir != null && dir.isFile()) {
            size = dir.length();
        }
        return size;
    }

    private String formatSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private void clearAppCache() {
        try {
            File dir = getCacheDir();
            long cacheSizeBefore = getDirSize(dir);
            if (deleteDir(dir)) {
                String freedSize = formatSize(cacheSizeBefore);
                Toast.makeText(this, "Cleared " + freedSize + " of Cache.", Toast.LENGTH_SHORT).show();
                AppLogger.d("App cache cleared successfully");
                updateCacheSizeText();
            } else {
                Toast.makeText(this, "Error clearing Cache.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            AppLogger.e("SettingsActivity", "Error clearing cache", e);
        }
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }
}
