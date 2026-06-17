package com.tuhoang.pocketmind.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.CameraAlt
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.ui.components.LoadingOverlay
import com.tuhoang.pocketmind.ui.components.rememberShowSnackbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: ProfileEditViewModel = viewModel()
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = viewModel.currentUser()
    val isLoading by viewModel.isLoading.collectAsState()
    val showSnackbar = rememberShowSnackbar()

    var name by remember(user?.uid) { mutableStateOf(user?.displayName ?: "") }
    var avatarUri by remember(user?.uid) { mutableStateOf<Uri?>(user?.photoUrl) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteKeyword by remember { mutableStateOf("") }
    var deleteConfirmed by remember { mutableStateOf(false) }
    var localLoading by remember { mutableStateOf(false) }

    val cropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { cropped ->
                avatarUri = cropped
                viewModel.uploadAvatar(cropped) {
                    auth.currentUser?.photoUrl?.let { avatarUri = it }
                }
            }
        } else {
            result.error?.message?.let { showSnackbar(it) }
        }
    }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            cropLauncher.launch(
                CropImageContractOptions(
                    uri = it,
                    cropImageOptions = CropImageOptions(
                        cropShape = CropImageView.CropShape.OVAL,
                        aspectRatioX = 1,
                        aspectRatioY = 1,
                        fixAspectRatio = true,
                        guidelines = CropImageView.Guidelines.ON
                    )
                )
            )
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.message.collect { msg ->
            msg?.let {
                showSnackbar(it)
                viewModel.consumeMessage()
            }
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.error.collect { err ->
            err?.let {
                showSnackbar(it)
                viewModel.consumeError()
            }
        }
    }

    if (user == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    if (isLoading || localLoading) {
        LoadingOverlay(stringResource(R.string.profile_processing))
        return
    }

    val deleteKeywordExpected = stringResource(R.string.profile_delete_keyword)
    val deleteInvalidMsg = stringResource(R.string.profile_delete_invalid)
    val deleteSuccessMsg = stringResource(R.string.profile_delete_success)
    val deleteErrorMsg = stringResource(R.string.profile_delete_error)

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
                    if (deleteKeyword != deleteKeywordExpected || !deleteConfirmed) {
                        showSnackbar(deleteInvalidMsg)
                        return@TextButton
                    }
                    localLoading = true
                    db.collection("users").document(user.uid)
                        .update("status", "pending_delete")
                        .addOnSuccessListener {
                            auth.signOut()
                            localLoading = false
                            showDeleteDialog = false
                            showSnackbar(deleteSuccessMsg)
                            onDeleted()
                        }
                        .addOnFailureListener {
                            localLoading = false
                            showSnackbar(deleteErrorMsg)
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
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .clickable { pickLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (avatarUri != null) {
                    AsyncImage(
                        model = avatarUri,
                        contentDescription = stringResource(R.string.cd_avatar),
                        modifier = Modifier.fillMaxSize(),
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
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = stringResource(R.string.profile_change_avatar),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = stringResource(R.string.profile_tap_avatar),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

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
                onClick = { viewModel.saveProfile(name, onBack) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) { Text(stringResource(R.string.action_save)) }

            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) { Text(stringResource(R.string.profile_delete_account)) }
        }
    }
}
