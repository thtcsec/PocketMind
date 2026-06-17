package com.tuhoang.pocketmind.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.ui.components.AboutDialog
import com.tuhoang.pocketmind.ui.components.SectionCard
import com.tuhoang.pocketmind.utils.AppInfo

@Composable
fun ProfileScreen(
    onNavigateLogin: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateProfileEdit: () -> Unit,
    onNavigateAdmin: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsState()
    val role by viewModel.userRole.collectAsState()
    val stats by viewModel.stats.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val versionInfo = remember { AppInfo.versionInfo(context) }

    LaunchedEffect(Unit) { viewModel.fetchUserData() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.fetchUserData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.profile_logout_title)) },
            text = { Text(stringResource(R.string.profile_logout_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.logout()
                    showLogoutDialog = false
                }) { Text(stringResource(R.string.action_logout)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = stringResource(R.string.title_profile)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (user?.photoUrl != null) {
                    AsyncImage(
                        model = user?.photoUrl,
                        contentDescription = stringResource(R.string.cd_avatar),
                        modifier = Modifier.size(96.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_profile),
                        contentDescription = stringResource(R.string.cd_avatar),
                        modifier = Modifier.size(96.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = user?.displayName?.takeIf { !it.isNullOrEmpty() } ?: stringResource(R.string.guest),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    text = user?.email ?: stringResource(R.string.please_login_to_sync),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (user != null) {
            SectionCard(title = stringResource(R.string.profile_stats_title)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        icon = { Icon(Icons.Default.Insights, contentDescription = null) },
                        value = stats.transactionCount.toString(),
                        label = stringResource(R.string.profile_stat_transactions),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = { Icon(Icons.Default.Verified, contentDescription = null) },
                        value = stats.planName,
                        label = stringResource(R.string.profile_stat_plan),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (user == null) {
            Button(onClick = onNavigateLogin, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_login))
            }
        } else {
            SectionCard(title = stringResource(R.string.section_account_actions)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onNavigateSettings, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Text(stringResource(R.string.title_settings), modifier = Modifier.padding(start = 8.dp))
                    }
                    OutlinedButton(onClick = onNavigateProfileEdit, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Person, contentDescription = null)
                        Text(stringResource(R.string.profile_edit_title), modifier = Modifier.padding(start = 8.dp))
                    }

                    if (role == "admin") {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        OutlinedButton(onClick = onNavigateAdmin, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                            Text(stringResource(R.string.admin_dashboard_title), modifier = Modifier.padding(start = 8.dp))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    OutlinedButton(onClick = { showLogoutDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Text(stringResource(R.string.action_logout), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAboutDialog = true },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.about_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.about_version_format, versionInfo.versionName, versionInfo.versionCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: @Composable () -> Unit,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            icon()
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
