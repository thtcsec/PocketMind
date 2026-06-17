package com.tuhoang.pocketmind.ui.profile

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.ui.components.LoadingOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(onBack: () -> Unit, onDeleted: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser

    var name by remember { mutableStateOf(user?.displayName ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteKeyword by remember { mutableStateOf("") }
    var deleteConfirmed by remember { mutableStateOf(false) }

    if (user == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    if (isLoading) {
        LoadingOverlay(stringResource(R.string.profile_processing))
        return
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.profile_delete_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.profile_delete_message))
                    OutlinedTextField(
                        value = deleteKeyword,
                        onValueChange = { deleteKeyword = it },
                        label = { Text(stringResource(R.string.profile_delete_hint)) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = deleteConfirmed, onCheckedChange = { deleteConfirmed = it })
                        Text(stringResource(R.string.profile_delete_confirm))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val expected = context.getString(R.string.profile_delete_keyword)
                    if (deleteKeyword != expected || !deleteConfirmed) {
                        Toast.makeText(context, R.string.profile_delete_invalid, Toast.LENGTH_LONG).show()
                        return@TextButton
                    }
                    isLoading = true
                    db.collection("users").document(user.uid)
                        .update("status", "pending_delete")
                        .addOnSuccessListener {
                            auth.signOut()
                            isLoading = false
                            showDeleteDialog = false
                            Toast.makeText(context, R.string.profile_delete_success, Toast.LENGTH_LONG).show()
                            onDeleted()
                        }
                        .addOnFailureListener {
                            isLoading = false
                            Toast.makeText(context, R.string.profile_delete_error, Toast.LENGTH_SHORT).show()
                        }
                }) { Text(stringResource(R.string.profile_delete_account)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (user.photoUrl != null) {
                AsyncImage(
                    model = user.photoUrl,
                    contentDescription = stringResource(R.string.cd_avatar),
                    modifier = Modifier.size(80.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_profile),
                    contentDescription = stringResource(R.string.cd_avatar),
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.label_name)) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )
            OutlinedTextField(
                value = user.email ?: "",
                onValueChange = {},
                label = { Text(stringResource(R.string.label_email)) },
                enabled = false,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, R.string.profile_name_required, Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    val updates = UserProfileChangeRequest.Builder().setDisplayName(name).build()
                    user.updateProfile(updates).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            db.collection("users").document(user.uid).update("name", name)
                                .addOnSuccessListener {
                                    isLoading = false
                                    Toast.makeText(context, R.string.profile_save_success, Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                                .addOnFailureListener {
                                    isLoading = false
                                    Toast.makeText(context, R.string.profile_save_error_db, Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            isLoading = false
                            Toast.makeText(context, R.string.profile_save_error_auth, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) { Text(stringResource(R.string.action_save)) }

            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) { Text(stringResource(R.string.profile_delete_account)) }
        }
    }
}
