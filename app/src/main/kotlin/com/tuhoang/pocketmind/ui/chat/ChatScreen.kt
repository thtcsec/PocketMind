package com.tuhoang.pocketmind.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.data.models.ChatMessage
import com.tuhoang.pocketmind.ui.components.CategoryPicker
import com.tuhoang.pocketmind.ui.components.SectionCard
import com.tuhoang.pocketmind.ui.components.dismissKeyboardOnTap
import com.tuhoang.pocketmind.ui.components.rememberShowSnackbar
import com.tuhoang.pocketmind.utils.CategoryUtils
import com.tuhoang.pocketmind.utils.HapticUtils
import com.tuhoang.pocketmind.utils.MoneyFormatter
import com.tuhoang.pocketmind.utils.PrefsManager
import com.tuhoang.pocketmind.utils.rememberMoneyFormatter
import java.io.File

@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val context = LocalContext.current
    val showSnackbar = rememberShowSnackbar()
    val messages by viewModel.messages.collectAsState()
    val errorEvents by viewModel.errorEvents.collectAsState()
    val infoEvents by viewModel.infoEvents.collectAsState()
    val isSavingManual by viewModel.isSavingManual.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()
    val isLoadingOlder by viewModel.isLoadingOlder.collectAsState()
    val hasOlderMessages by viewModel.hasOlderMessages.collectAsState()
    val money = rememberMoneyFormatter()

    var modeIndex by remember { mutableIntStateOf(0) }
    var inputText by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    var showChatMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.chat_clear_title)) },
            text = { Text(stringResource(R.string.chat_clear_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearChatHistory { showClearDialog = false }
                }) { Text(stringResource(R.string.chat_clear_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    LaunchedEffect(Unit) { viewModel.startListeningForMessages() }

    LaunchedEffect(errorEvents) {
        errorEvents?.let {
            showSnackbar(it)
            viewModel.consumeError()
        }
    }

    LaunchedEffect(infoEvents) {
        infoEvents?.let {
            showSnackbar(it)
            viewModel.consumeInfo()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.uploadImageAndSend(it) } }

    val requestMicLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showSnackbar(context.getString(R.string.add_voice_recording))
        } else {
            showSnackbar(context.getString(R.string.error_mic_permission))
        }
    }

    val requestStorageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pickImageLauncher.launch("image/*")
        else showSnackbar(context.getString(R.string.error_camera_permission))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .dismissKeyboardOnTap()
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            SegmentedButton(
                selected = modeIndex == 0,
                onClick = { modeIndex = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text(stringResource(R.string.chat_mode_manual)) }
            SegmentedButton(
                selected = modeIndex == 1,
                onClick = { modeIndex = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text(stringResource(R.string.chat_mode_ai)) }
        }

        if (modeIndex == 0) {
            ManualEntryFields(
                isSaving = isSavingManual,
                money = money,
                onSave = { amount, type, category, note, receiptUri ->
                    HapticUtils.performClick(context)
                    viewModel.saveManualTransaction(amount, type, category, note, receiptUri)
                },
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.chat_mode_ai),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
                Box {
                    IconButton(onClick = { showChatMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.chat_options))
                    }
                    DropdownMenu(expanded = showChatMenu, onDismissRequest = { showChatMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_load_older)) },
                            onClick = {
                                showChatMenu = false
                                viewModel.loadOlderMessages()
                            },
                            enabled = hasOlderMessages && !isLoadingOlder
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_clear_history)) },
                            onClick = {
                                showChatMenu = false
                                showClearDialog = true
                            }
                        )
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hasOlderMessages) {
                    item {
                        TextButton(
                            onClick = { viewModel.loadOlderMessages() },
                            enabled = !isLoadingOlder,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isLoadingOlder) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text(stringResource(R.string.chat_load_older))
                            }
                        }
                    }
                }
                items(messages) { msg -> ChatBubble(msg) }
                if (isAiThinking) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text(stringResource(R.string.chat_ai_thinking))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val storageEnabled = PrefsManager.getInstance().isStorageEnabled(
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            ) == PackageManager.PERMISSION_GRANTED
                    )
                    if (!storageEnabled) {
                        showSnackbar(context.getString(R.string.error_storage_disabled))
                        return@IconButton
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        pickImageLauncher.launch("image/*")
                    } else {
                        requestStorageLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }) {
                    Icon(Icons.Default.AttachFile, contentDescription = stringResource(R.string.cd_upload_image))
                }
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.chat_input_placeholder)) },
                    maxLines = 3,
                    enabled = !isAiThinking
                )
                if (inputText.isBlank()) {
                    IconButton(onClick = {
                        val micEnabled = PrefsManager.getInstance().isMicEnabled(
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                        )
                        if (!micEnabled) {
                            showSnackbar(context.getString(R.string.error_mic_disabled))
                            return@IconButton
                        }
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            showSnackbar(context.getString(R.string.add_voice_recording))
                        } else {
                            requestMicLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }) {
                        Icon(Icons.Default.Mic, contentDescription = stringResource(R.string.cd_voice_input))
                    }
                } else {
                    IconButton(
                        onClick = {
                            HapticUtils.performClick(context)
                            viewModel.sendMessage(inputText.trim())
                            inputText = ""
                        },
                        enabled = !isAiThinking
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.cd_send_message))
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualEntryFields(
    isSaving: Boolean,
    money: MoneyFormatter,
    onSave: (amount: String, type: String, category: String, note: String, receiptUri: Uri?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = PrefsManager.getInstance()
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var typeIndex by remember { mutableIntStateOf(0) }
    var receiptUri by remember { mutableStateOf<Uri?>(null) }
    var categories by remember { mutableStateOf(CategoryUtils.allCategories(context)) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        receiptUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) receiptUri = pendingCameraUri
        pendingCameraUri = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val file = File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    SectionCard(title = stringResource(R.string.section_manual_entry), modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = typeIndex == 0,
                    onClick = { typeIndex = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text(stringResource(R.string.chat_type_expense)) }
                SegmentedButton(
                    selected = typeIndex == 1,
                    onClick = { typeIndex = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text(stringResource(R.string.chat_type_income)) }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                label = { Text(stringResource(R.string.label_amount)) },
                supportingText = {
                    amount.toDoubleOrNull()?.let {
                        Text(money.format(it))
                    }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text(
                text = stringResource(R.string.label_category),
                style = MaterialTheme.typography.labelLarge
            )
            CategoryPicker(
                categories = categories,
                selected = category,
                onSelected = { category = it },
                onAddCustom = { name ->
                    CategoryUtils.addCustomCategory(prefs, name)
                    categories = CategoryUtils.allCategories(context)
                }
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.label_note)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Text(
                text = stringResource(R.string.manual_receipt_photo),
                style = MaterialTheme.typography.labelLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { launchCamera() }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.manual_take_photo), modifier = Modifier.padding(start = 6.dp))
                }
                OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.manual_pick_gallery), modifier = Modifier.padding(start = 6.dp))
                }
            }
            receiptUri?.let { uri ->
                Box {
                    AsyncImage(
                        model = uri,
                        contentDescription = stringResource(R.string.manual_receipt_photo),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { receiptUri = null },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.manual_remove_photo))
                    }
                }
            }

            Button(
                onClick = {
                    val type = if (typeIndex == 0) "expense" else "income"
                    onSave(amount, type, category, note, receiptUri)
                    amount = ""
                    category = ""
                    note = ""
                    receiptUri = null
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val colors = if (message.isUser) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Card(
            modifier = Modifier.padding(vertical = 2.dp).fillMaxWidth(0.8f),
            shape = RoundedCornerShape(12.dp),
            colors = colors
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.imageUrl != null) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = stringResource(R.string.cd_chat_image),
                        modifier = Modifier.fillMaxWidth().size(180.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(text = message.content, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
