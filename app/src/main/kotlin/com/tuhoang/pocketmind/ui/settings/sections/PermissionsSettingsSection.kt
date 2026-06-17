package com.tuhoang.pocketmind.ui.settings.sections

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.ui.settings.components.SettingsSectionHeader
import com.tuhoang.pocketmind.ui.settings.components.SettingsToggleRow
import com.tuhoang.pocketmind.ui.settings.components.hasPermission
import com.tuhoang.pocketmind.utils.PrefsManager

@Composable
fun PermissionsSettingsSection(prefs: PrefsManager) {
    val context = LocalContext.current

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

    SettingsSectionHeader(stringResource(R.string.settings_permissions))
    SettingsToggleRow(stringResource(R.string.settings_permissions_camera), cameraEnabled) { checked ->
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
    SettingsToggleRow(stringResource(R.string.settings_permissions_storage), storageEnabled) { checked ->
        if (checked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
                hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
            ) {
                storageEnabled = true
                prefs.setStorageEnabled(true)
            } else storageLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            storageEnabled = false
            prefs.setStorageEnabled(false)
        }
    }
    SettingsToggleRow(stringResource(R.string.settings_permissions_mic), micEnabled) { checked ->
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
}
