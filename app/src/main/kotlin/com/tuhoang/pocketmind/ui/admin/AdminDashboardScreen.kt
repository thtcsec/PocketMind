package com.tuhoang.pocketmind.ui.admin

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.utils.AppLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var isAuthorized by remember { mutableStateOf<Boolean?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var users by remember { mutableStateOf<List<DocumentSnapshot>>(emptyList()) }
    var plans by remember { mutableStateOf<List<DocumentSnapshot>>(emptyList()) }
    var editUser by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var showWorkerDialog by remember { mutableStateOf(false) }
    var workerUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            isAuthorized = false
            return@LaunchedEffect
        }
        isLoading = true
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                isAuthorized = doc.exists() && doc.getString("role") == "admin"
                isLoading = false
                if (isAuthorized == true) loadUsers(db) { users = it }
            }
            .addOnFailureListener {
                isAuthorized = false
                isLoading = false
            }
    }

    LaunchedEffect(selectedTab, isAuthorized) {
        if (isAuthorized != true) return@LaunchedEffect
        isLoading = true
        if (selectedTab == 0) {
            loadUsers(db) { users = it; isLoading = false }
        } else {
            loadPlans(db) { plans = it; isLoading = false }
        }
    }

    if (isAuthorized == false) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, R.string.admin_err_no_access, Toast.LENGTH_LONG).show()
            onBack()
        }
        return
    }

    if (isAuthorized == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    editUser?.let { userDoc ->
        var role by remember { mutableStateOf(userDoc.getString("role") ?: "user") }
        var limit by remember { mutableStateOf(userDoc.getLong("ai_chat_limit")?.toString() ?: "5") }
        AlertDialog(
            onDismissRequest = { editUser = null },
            title = { Text(stringResource(R.string.admin_edit_user_title, userDoc.getString("name") ?: "")) },
            text = {
                Column {
                    OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text(stringResource(R.string.admin_role_label)) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = limit, onValueChange = { limit = it }, label = { Text(stringResource(R.string.admin_limit_label)) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val updates = mutableMapOf<String, Any>()
                    if (role.isNotBlank()) updates["role"] = role
                    limit.toLongOrNull()?.let { updates["ai_chat_limit"] = it }
                    if (updates.isNotEmpty()) {
                        db.collection("users").document(userDoc.id).update(updates)
                            .addOnSuccessListener {
                                Toast.makeText(context, R.string.admin_user_updated, Toast.LENGTH_SHORT).show()
                                editUser = null
                                loadUsers(db) { users = it }
                            }
                    }
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { editUser = null }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }

    if (showWorkerDialog) {
        AlertDialog(
            onDismissRequest = { showWorkerDialog = false },
            title = { Text(stringResource(R.string.admin_global_config_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.admin_global_config_desc))
                    OutlinedTextField(
                        value = workerUrl,
                        onValueChange = { workerUrl = it },
                        label = { Text(stringResource(R.string.admin_worker_url_label)) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (workerUrl.isNotBlank()) {
                        val data = mapOf(
                            "worker_url" to workerUrl.trim(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        db.collection("system_configs").document("global").set(data, SetOptions.merge())
                            .addOnSuccessListener {
                                Toast.makeText(context, R.string.admin_worker_updated, Toast.LENGTH_SHORT).show()
                                showWorkerDialog = false
                            }
                    }
                }) { Text(stringResource(R.string.admin_save_globally)) }
            },
            dismissButton = { TextButton(onClick = { showWorkerDialog = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_dashboard_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isLoading = true
                        db.collection("system_configs").document("global").get()
                            .addOnSuccessListener { doc ->
                                workerUrl = doc.getString("worker_url") ?: ""
                                showWorkerDialog = true
                                isLoading = false
                            }
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.admin_config_worker))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                stringResource(R.string.admin_dashboard_desc),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(stringResource(R.string.admin_tab_users)) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(stringResource(R.string.admin_tab_plans)) })
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (selectedTab == 0) {
                LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(users) { doc ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(doc.getString("name") ?: stringResource(R.string.admin_unknown), style = MaterialTheme.typography.titleMedium)
                                Text(doc.getString("email") ?: stringResource(R.string.admin_no_email), style = MaterialTheme.typography.bodySmall)
                                Text(
                                    stringResource(
                                        R.string.admin_user_role_limit,
                                        doc.getString("role") ?: "user",
                                        doc.getLong("ai_chat_limit")?.toString() ?: "?"
                                    )
                                )
                                Button(onClick = { editUser = doc }, modifier = Modifier.padding(top = 8.dp)) {
                                    Text(stringResource(R.string.admin_edit))
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(plans) { doc ->
                        val isActive = doc.getBoolean("is_active") ?: false
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(doc.getString("name") ?: stringResource(R.string.admin_unknown_plan), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (isActive) stringResource(R.string.admin_plan_active) else stringResource(R.string.admin_plan_inactive),
                                    color = if (isActive) Color(0xFF4CAF50) else Color(0xFFF44336)
                                )
                                Button(
                                    onClick = {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.admin_edit_plan_toast, doc.getString("name") ?: ""),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) { Text(stringResource(R.string.admin_edit)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun loadUsers(db: FirebaseFirestore, onResult: (List<DocumentSnapshot>) -> Unit) {
    db.collection("users").get()
        .addOnSuccessListener { onResult(it.documents) }
        .addOnFailureListener { e -> AppLogger.e("AdminDashboard", "Failed to load users", e) }
}

private fun loadPlans(db: FirebaseFirestore, onResult: (List<DocumentSnapshot>) -> Unit) {
    db.collection("ai_plans").get()
        .addOnSuccessListener { onResult(it.documents) }
        .addOnFailureListener { e -> AppLogger.e("AdminDashboard", "Failed to load plans", e) }
}
