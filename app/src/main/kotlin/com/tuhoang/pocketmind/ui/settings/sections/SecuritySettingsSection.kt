package com.tuhoang.pocketmind.ui.settings.sections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.ui.settings.components.SettingsSectionHeader
import com.tuhoang.pocketmind.ui.settings.components.SettingsToggleRow
import com.tuhoang.pocketmind.utils.PrefsManager

@Composable
fun SecuritySettingsSection(prefs: PrefsManager) {
    var biometricEnabled by remember { mutableStateOf(prefs.isBiometricEnabled()) }
    var dynamicColorEnabled by remember { mutableStateOf(prefs.isDynamicColorEnabled()) }
    var hapticEnabled by remember { mutableStateOf(prefs.isHapticEnabled()) }

    SettingsSectionHeader(stringResource(R.string.settings_security))
    SettingsToggleRow(stringResource(R.string.settings_biometric), biometricEnabled) { checked ->
        biometricEnabled = checked
        prefs.setBiometricEnabled(checked)
    }
    SettingsToggleRow(stringResource(R.string.settings_dynamic_color), dynamicColorEnabled) { checked ->
        dynamicColorEnabled = checked
        prefs.setDynamicColorEnabled(checked)
    }
    SettingsToggleRow(stringResource(R.string.settings_haptic), hapticEnabled) { checked ->
        hapticEnabled = checked
        prefs.setHapticEnabled(checked)
    }
}
