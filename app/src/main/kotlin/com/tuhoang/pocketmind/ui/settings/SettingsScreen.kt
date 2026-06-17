package com.tuhoang.pocketmind.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import com.google.firebase.auth.FirebaseAuth
import com.tuhoang.pocketmind.PocketMindApp
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.utils.PrefsManager
import java.io.File
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onNavigateAiSettings: () -> Unit) {
    val context = LocalContext.current
    val prefs = PrefsManager.getInstance()

    var themePref by remember { mutableIntStateOf(prefs.getTheme(PocketMindApp.THEME_SYSTEM)) }
    var currency by remember { mutableStateOf(prefs.getCurrency("USD")) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var cacheSize by remember { mutableStateOf(formatSize(getDirSize(context.cacheDir))) }
    var budgetInput by remember { mutableStateOf(prefs.getMonthlyBudget().toLong().toString()) }

    var biometricEnabled by remember { mutableStateOf(prefs.isBiometricEnabled()) }
    var dynamicColorEnabled by remember { mutableStateOf(prefs.isDynamicColorEnabled()) }
    var hapticEnabled by remember { mutableStateOf(prefs.isHapticEnabled()) }

    var cameraEnabled by remember {
        mutableStateOf(prefs.isCameraEnabled(hasPermission(context, Manifest.permission.CAMERA)))
    }
    var storageEnabled by remember {
        mutableStateOf(prefs.isStorageEnabled(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
                hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        ))
    }
    var micEnabled by remember {
        mutableStateOf(prefs.isMicEnabled(hasPermission(context, Manifest.permission.RECORD_AUDIO)))
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraEnabled = granted
        prefs.setCameraEnabled(granted)
    }
    val storageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        storageEnabled = granted
        prefs.setStorageEnabled(granted)
    }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micEnabled = granted
        prefs.setMicEnabled(granted)
    }

    val themeLabel = when (themePref) {
        PocketMindApp.THEME_LIGHT -> stringResource(R.string.theme_light)
        PocketMindApp.THEME_DARK -> stringResource(R.string.theme_dark)
        else -> stringResource(R.string.theme_system)
    }

    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val languageLabel = if (!currentLocales.isEmpty && currentLocales[0]?.language == "vi") {
        stringResource(R.string.vietnamese)
    } else {
        stringResource(R.string.english)
    }

    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
                ?: context.getString(R.string.settings_unknown_version)
        } catch (_: PackageManager.NameNotFoundException) {
            context.getString(R.string.settings_unknown_version)
        }
    }

    if (showThemeDialog) {
        val themes = listOf(
            stringResource(R.string.theme_system),
            stringResource(R.string.theme_light),
            stringResource(R.string.theme_dark)
        )
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.settings_theme)) },
            text = {
                Column {
                    themes.forEachIndexed { index, label ->
                        TextButton(onClick = {
                            themePref = index
                            prefs.setTheme(index)
                            when (index) {
                                PocketMindApp.THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                                PocketMindApp.THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                            }
                            showThemeDialog = false
                        }) { Text(label) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_select_language)) },
            text = {
                Column {
                    TextButton(onClick = {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                        showLanguageDialog = false
                    }) { Text(stringResource(R.string.english)) }
                    TextButton(onClick = {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("vi"))
                        showLanguageDialog = false
                    }) { Text(stringResource(R.string.vietnamese)) }
                }
            },
            confirmButton = { TextButton(onClick = { showLanguageDialog = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }

    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text(stringResource(R.string.settings_monthly_budget)) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.settings_budget_amount)) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val budget = budgetInput.toDoubleOrNull() ?: 0.0
                    prefs.setMonthlyBudget(budget)
                    showBudgetDialog = false
                    Toast.makeText(context, R.string.settings_budget_saved, Toast.LENGTH_SHORT).show()
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showCurrencyDialog) {
        val currencies = listOf("USD", "AUD", "JPY", "VND")
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text(stringResource(R.string.settings_currency)) },
            text = {
                Column {
                    currencies.forEach { code ->
                        TextButton(onClick = {
                            currency = code
                            prefs.setCurrency(code)
                            showCurrencyDialog = false
                        }) { Text(code) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCurrencyDialog = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            SectionHeader(stringResource(R.string.settings_appearance_language))
            SettingsRow(stringResource(R.string.settings_theme), themeLabel) { showThemeDialog = true }
            SettingsRow(stringResource(R.string.settings_language), languageLabel) { showLanguageDialog = true }
            HorizontalDivider()

            SectionHeader(stringResource(R.string.settings_data_storage))
            SettingsRow(stringResource(R.string.settings_currency), currency) { showCurrencyDialog = true }
            SettingsRow(stringResource(R.string.settings_monthly_budget), budgetInput.ifBlank { "0" }) {
                budgetInput = prefs.getMonthlyBudget().toLong().toString()
                showBudgetDialog = true
            }
            SettingsRow(stringResource(R.string.settings_clear_cache), cacheSize) {
                if (deleteDir(context.cacheDir)) {
                    cacheSize = formatSize(getDirSize(context.cacheDir))
                    Toast.makeText(context, R.string.settings_cache_cleared, Toast.LENGTH_SHORT).show()
                }
            }
            HorizontalDivider()

            SectionHeader(stringResource(R.string.settings_ai_section))
            SettingsRow(stringResource(R.string.settings_ai_config), "") {
                if (FirebaseAuth.getInstance().currentUser != null) onNavigateAiSettings()
                else Toast.makeText(context, R.string.you_must_login_to_configure, Toast.LENGTH_SHORT).show()
            }
            HorizontalDivider()

            SectionHeader(stringResource(R.string.settings_security))
            PermissionSwitch(stringResource(R.string.settings_biometric), biometricEnabled) { checked ->
                biometricEnabled = checked
                prefs.setBiometricEnabled(checked)
            }
            PermissionSwitch(stringResource(R.string.settings_dynamic_color), dynamicColorEnabled) { checked ->
                dynamicColorEnabled = checked
                prefs.setDynamicColorEnabled(checked)
            }
            PermissionSwitch(stringResource(R.string.settings_haptic), hapticEnabled) { checked ->
                hapticEnabled = checked
                prefs.setHapticEnabled(checked)
            }
            HorizontalDivider()

            SectionHeader(stringResource(R.string.settings_permissions))
            PermissionSwitch(stringResource(R.string.settings_permissions_camera), cameraEnabled) { checked ->
                if (checked) {
                    if (hasPermission(context, Manifest.permission.CAMERA)) {
                        cameraEnabled = true
                        prefs.setCameraEnabled(true)
                    } else cameraLauncher.launch(Manifest.permission.CAMERA)
                } else {
                    cameraEnabled = false
                    prefs.setCameraEnabled(false)
                }
            }
            PermissionSwitch(stringResource(R.string.settings_permissions_storage), storageEnabled) { checked ->
                if (checked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        storageEnabled = true
                        prefs.setStorageEnabled(true)
                    } else storageLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                } else {
                    storageEnabled = false
                    prefs.setStorageEnabled(false)
                }
            }
            PermissionSwitch(stringResource(R.string.settings_permissions_mic), micEnabled) { checked ->
                if (checked) {
                    if (hasPermission(context, Manifest.permission.RECORD_AUDIO)) {
                        micEnabled = true
                        prefs.setMicEnabled(true)
                    } else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    micEnabled = false
                    prefs.setMicEnabled(false)
                }
            }
            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_version, versionName)) },
                supportingContent = { Text(stringResource(R.string.settings_developer)) }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SettingsRow(label: String, value: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { if (value.isNotEmpty()) Text(value) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    )
}

@Composable
private fun PermissionSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) }
    )
}

private fun hasPermission(context: android.content.Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun getDirSize(dir: File?): Long {
    if (dir == null) return 0
    if (dir.isFile) return dir.length()
    return dir.listFiles()?.sumOf { if (it.isFile) it.length() else getDirSize(it) } ?: 0
}

private fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}

private fun deleteDir(dir: File?): Boolean {
    if (dir == null) return false
    if (dir.isDirectory) {
        dir.list()?.forEach { child ->
            if (!deleteDir(File(dir, child))) return false
        }
    }
    return dir.delete()
}
