package com.tuhoang.pocketmind.ui.settings.sections

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import com.tuhoang.pocketmind.PocketMindApp
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.ui.settings.components.SettingsNavRow
import com.tuhoang.pocketmind.ui.settings.components.SettingsSectionHeader
import com.tuhoang.pocketmind.utils.PrefsManager

@Composable
fun AppearanceSettingsSection(prefs: PrefsManager) {
    var themePref by remember { mutableIntStateOf(prefs.getTheme(PocketMindApp.THEME_SYSTEM)) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

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
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
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
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    SettingsSectionHeader(stringResource(R.string.settings_appearance_language))
    SettingsNavRow(stringResource(R.string.settings_theme), themeLabel) { showThemeDialog = true }
    SettingsNavRow(stringResource(R.string.settings_language), languageLabel) { showLanguageDialog = true }
}
