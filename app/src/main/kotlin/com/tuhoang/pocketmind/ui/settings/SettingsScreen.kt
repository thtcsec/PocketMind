package com.tuhoang.pocketmind.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.firebase.auth.FirebaseAuth
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.ui.components.AboutDialog
import com.tuhoang.pocketmind.ui.settings.components.SettingsNavRow
import com.tuhoang.pocketmind.ui.settings.components.SettingsSectionHeader
import com.tuhoang.pocketmind.ui.settings.sections.AppearanceSettingsSection
import com.tuhoang.pocketmind.ui.settings.sections.DataStorageSettingsSection
import com.tuhoang.pocketmind.ui.settings.sections.PermissionsSettingsSection
import com.tuhoang.pocketmind.ui.settings.sections.SecuritySettingsSection
import com.tuhoang.pocketmind.utils.AppInfo
import com.tuhoang.pocketmind.utils.PrefsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onNavigateAiSettings: () -> Unit) {
    val context = LocalContext.current
    val prefs = PrefsManager.getInstance()
    var showAboutDialog by remember { mutableStateOf(false) }

    val versionInfo = remember { AppInfo.versionInfo(context) }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            AppearanceSettingsSection(prefs)
            HorizontalDivider()

            DataStorageSettingsSection(prefs)
            HorizontalDivider()

            SettingsSectionHeader(stringResource(R.string.settings_ai_section))
            SettingsNavRow(stringResource(R.string.settings_ai_config), "") {
                if (FirebaseAuth.getInstance().currentUser != null) onNavigateAiSettings()
                else Toast.makeText(context, R.string.you_must_login_to_configure, Toast.LENGTH_SHORT).show()
            }
            HorizontalDivider()

            SecuritySettingsSection(prefs)
            HorizontalDivider()

            PermissionsSettingsSection(prefs)
            HorizontalDivider()

            ListItem(
                modifier = Modifier.clickable { showAboutDialog = true },
                headlineContent = {
                    Text(stringResource(R.string.settings_version, versionInfo.versionName))
                },
                supportingContent = { Text(stringResource(R.string.settings_developer)) }
            )
        }
    }
}
