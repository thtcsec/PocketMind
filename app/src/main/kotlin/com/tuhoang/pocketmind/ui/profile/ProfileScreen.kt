package com.tuhoang.pocketmind.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.tuhoang.pocketmind.R

@Composable
fun ProfileScreen(
    onNavigateLogin: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateProfileEdit: () -> Unit,
    onNavigateAdmin: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val user by viewModel.currentUser.collectAsState()
    val role by viewModel.userRole.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.fetchUserData() }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.profile_logout_title)) },
            text = { Text(stringResource(R.string.profile_logout_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.logout()
                    showLogoutDialog = false
                }) { Text(stringResource(R.string.profile_logout_title)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (user?.photoUrl != null) {
            AsyncImage(
                model = user?.photoUrl,
                contentDescription = "Avatar",
                modifier = Modifier.size(96.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_profile),
                contentDescription = "Avatar",
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = user?.displayName?.takeIf { !it.isNullOrEmpty() } ?: stringResource(R.string.guest),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = user?.email ?: stringResource(R.string.please_login_to_sync),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (user == null) {
            Button(onClick = onNavigateLogin, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_login))
            }
        } else {
            OutlinedButton(onClick = onNavigateSettings, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Text(stringResource(R.string.title_settings), modifier = Modifier.padding(start = 8.dp))
            }
            OutlinedButton(onClick = onNavigateProfileEdit, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Person, contentDescription = null)
                Text("Edit Profile", modifier = Modifier.padding(start = 8.dp))
            }

            if (role == "admin") {
                HorizontalDivider()
                OutlinedButton(onClick = onNavigateAdmin, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                    Text(stringResource(R.string.admin_dashboard_title), modifier = Modifier.padding(start = 8.dp))
                }
            }

            HorizontalDivider()
            OutlinedButton(onClick = { showLogoutDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Text(stringResource(R.string.action_logout), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
