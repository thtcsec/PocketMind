package com.tuhoang.pocketmind.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.data.models.ChatMessage
import com.tuhoang.pocketmind.utils.PrefsManager

@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val errorEvents by viewModel.errorEvents.collectAsState()
    val infoEvents by viewModel.infoEvents.collectAsState()

    var modeIndex by remember { mutableIntStateOf(0) }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { viewModel.startListeningForMessages() }

    LaunchedEffect(errorEvents) {
        errorEvents?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeError()
        }
    }

    LaunchedEffect(infoEvents) {
        infoEvents?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, context.getString(R.string.add_voice_recording), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, context.getString(R.string.error_mic_permission), Toast.LENGTH_LONG).show()
        }
    }

    val requestStorageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pickImageLauncher.launch("image/*")
        else Toast.makeText(context, context.getString(R.string.error_camera_permission), Toast.LENGTH_LONG).show()
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            SegmentedButton(
                selected = modeIndex == 0,
                onClick = { modeIndex = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text("Manual") }
            SegmentedButton(
                selected = modeIndex == 1,
                onClick = { modeIndex = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text("AI Chat") }
        }

        if (modeIndex == 0) {
            ManualEntrySection(
                onSave = {
                    Toast.makeText(context, context.getString(R.string.add_saving_manual), Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg -> ChatBubble(msg) }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.clearChatHistory() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear")
                }
                IconButton(onClick = {
                    val storageEnabled = PrefsManager.getInstance().isStorageEnabled(
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    )
                    if (!storageEnabled) {
                        Toast.makeText(context, "Storage access is disabled in App Settings", Toast.LENGTH_SHORT).show()
                        return@IconButton
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    ) {
                        pickImageLauncher.launch("image/*")
                    } else {
                        requestStorageLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Upload")
                }
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    maxLines = 3
                )
                if (inputText.isBlank()) {
                    IconButton(onClick = {
                        val micEnabled = PrefsManager.getInstance().isMicEnabled(
                            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        )
                        if (!micEnabled) {
                            Toast.makeText(context, "Microphone is disabled in App Settings", Toast.LENGTH_SHORT).show()
                            return@IconButton
                        }
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(context, context.getString(R.string.add_voice_recording), Toast.LENGTH_SHORT).show()
                        } else {
                            requestMicLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice")
                    }
                } else {
                    IconButton(onClick = {
                        viewModel.sendMessage(inputText.trim())
                        inputText = ""
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualEntrySection(onSave: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var typeIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = typeIndex == 0,
                onClick = { typeIndex = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text("Expense") }
            SegmentedButton(
                selected = typeIndex == 1,
                onClick = { typeIndex = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text("Income") }
        }
        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("Save") }
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
                        contentDescription = "Chat image",
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
