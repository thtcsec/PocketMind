package com.tuhoang.pocketmind.ui.settings;

import android.content.Context;
import android.content.Intent;
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
import com.tuhoang.pocketmind.utils.PrefsManager;
import java.io.File;
import java.util.Objects;

import android.Manifest;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;

    private ActivityResultLauncher<String> requestCameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                binding.switchCamera.setChecked(isGranted);
                PrefsManager.getInstance().setCameraEnabled(isGranted);
            });

    private ActivityResultLauncher<String> requestStoragePermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                binding.switchStorage.setChecked(isGranted);
                PrefsManager.getInstance().setStorageEnabled(isGranted);
            });

    private ActivityResultLauncher<String> requestMicPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                binding.switchMic.setChecked(isGranted);
                PrefsManager.getInstance().setMicEnabled(isGranted);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Toolbar
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Setup Theme selection
        updateThemeText(PrefsManager.getInstance().getTheme(PocketMindApp.THEME_SYSTEM));
        
        binding.llTheme.setOnClickListener(v -> {
            showThemeDialog();
        });

        // Setup Language display text based on current LocaleListCompat
        LocaleListCompat currentAppLocales = AppCompatDelegate.getApplicationLocales();
        if (!currentAppLocales.isEmpty() && Objects.requireNonNull(currentAppLocales.get(0)).getLanguage().equals("vi")) {
            binding.tvLanguageStatus.setText(getText(R.string.vietnamese));
        } else {
            binding.tvLanguageStatus.setText(getText(R.string.english));
        }

        binding.llLanguage.setOnClickListener(v -> {
            showLanguageDialog();
        });

        // Setup Currency selection
        updateCurrencyText(PrefsManager.getInstance().getCurrency("USD"));
        
        binding.llCurrency.setOnClickListener(v -> {
            showCurrencyDialog();
        });

        // Setup AI Configuration Routing
        binding.llAiConfig.setOnClickListener(v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                startActivity(new Intent(SettingsActivity.this, AiSettingsActivity.class));
            } else {
                Toast.makeText(this, getText(R.string.you_must_login_to_configure), Toast.LENGTH_SHORT).show();
            }
        });

        binding.llClearCache.setOnClickListener(v -> {
            clearAppCache();
        });

        updateCacheSizeText();

        // Setup System Permissions Switches (App-level preference overriding OS)
        boolean isCameraGranted = hasPermission(Manifest.permission.CAMERA);
        binding.switchCamera.setChecked(PrefsManager.getInstance().isCameraEnabled(isCameraGranted));
        binding.switchCamera.setOnClickListener(v -> {
            boolean isChecked = binding.switchCamera.isChecked();
            if (isChecked) {
                if (!hasPermission(Manifest.permission.CAMERA)) {
                    binding.switchCamera.setChecked(false); // Revert until granted
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                } else {
                    PrefsManager.getInstance().setCameraEnabled(true);
                }
            } else {
                PrefsManager.getInstance().setCameraEnabled(false);
            }
        });

        boolean isStorageGranted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        binding.switchStorage.setChecked(PrefsManager.getInstance().isStorageEnabled(isStorageGranted));
        binding.switchStorage.setOnClickListener(v -> {
            boolean isChecked = binding.switchStorage.isChecked();
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PrefsManager.getInstance().setStorageEnabled(true);
                } else if (!hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    binding.switchStorage.setChecked(false); // Revert until granted
                    requestStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                } else {
                    PrefsManager.getInstance().setStorageEnabled(true);
                }
            } else {
                PrefsManager.getInstance().setStorageEnabled(false);
            }
        });

        boolean isMicGranted = hasPermission(Manifest.permission.RECORD_AUDIO);
        binding.switchMic.setChecked(PrefsManager.getInstance().isMicEnabled(isMicGranted));
        binding.switchMic.setOnClickListener(v -> {
            boolean isChecked = binding.switchMic.isChecked();
            if (isChecked) {
                if (!hasPermission(Manifest.permission.RECORD_AUDIO)) {
                    binding.switchMic.setChecked(false); // Revert until granted
                    requestMicPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
                } else {
                    PrefsManager.getInstance().setMicEnabled(true);
                }
            } else {
                PrefsManager.getInstance().setMicEnabled(false);
            }
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

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
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
        int currentTheme = PrefsManager.getInstance().getTheme(PocketMindApp.THEME_SYSTEM);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.settings_theme))
                .setSingleChoiceItems(themes, currentTheme, (dialog, which) -> {
                    PrefsManager.getInstance().setTheme(which);
                    
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
        String currentCurrency = PrefsManager.getInstance().getCurrency("USD");
        int checkedItem = -1;
        for (int i = 0; i < currencies.length; i++) {
            if (currencies[i].equals(currentCurrency)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.settings_currency))
                .setSingleChoiceItems(currencies, checkedItem, (dialog, which) -> {
                    PrefsManager.getInstance().setCurrency(currencies[which]);
                    updateCurrencyText(currencies[which]);
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
