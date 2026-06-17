package com.tuhoang.pocketmind.ui.chat

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.data.models.ChatMessage
import com.tuhoang.pocketmind.data.repository.TransactionRepository
import com.tuhoang.pocketmind.utils.AppLogger
import com.tuhoang.pocketmind.utils.PrefsManager
import com.tuhoang.pocketmind.utils.ValidationUtils
import com.tuhoang.pocketmind.utils.WorkerApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val app = getApplication<Application>()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _errorEvents = MutableStateFlow<String?>(null)
    val errorEvents: StateFlow<String?> = _errorEvents.asStateFlow()

    private val _infoEvents = MutableStateFlow<String?>(null)
    val infoEvents: StateFlow<String?> = _infoEvents.asStateFlow()

    private val _isSavingManual = MutableStateFlow(false)
    val isSavingManual: StateFlow<Boolean> = _isSavingManual.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private var chatListener: ListenerRegistration? = null

    fun startListeningForMessages() {
        val uid = auth.currentUser?.uid ?: return

        chatListener?.remove()
        chatListener = db.collection("users").document(uid).collection("chats")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    AppLogger.e("ChatViewModel", "Listen failed", error)
                    return@addSnapshotListener
                }
                if (value != null) {
                    _messages.value = value.toObjects(ChatMessage::class.java)
                }
            }
    }

    fun stopListening() {
        chatListener?.remove()
        chatListener = null
    }

    fun sendMessage(text: String) {
        val uid = auth.currentUser?.uid ?: return
        val userMsg = ChatMessage(text, true, System.currentTimeMillis())
        db.collection("users").document(uid).collection("chats").add(userMsg)
            .addOnSuccessListener { requestAiResponse(text) }
            .addOnFailureListener { e ->
                _errorEvents.value = app.getString(R.string.chat_send_failed, e.message ?: "")
            }
    }

    private fun requestAiResponse(userText: String) {
        val uid = auth.currentUser?.uid ?: return
        val workerUrl = PrefsManager.getInstance().getWorkerUrl()
        _isAiThinking.value = true

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    WorkerApiClient.postChat(workerUrl, uid, userText)
                }
                val replyText = when {
                    result.message != null -> result.message
                    result.success -> app.getString(R.string.chat_ai_saved)
                    workerUrl.isBlank() -> app.getString(R.string.chat_ai_response)
                    else -> app.getString(R.string.chat_ai_error)
                }
                val botMsg = ChatMessage(replyText, false, System.currentTimeMillis())
                db.collection("users").document(uid).collection("chats").add(botMsg)

                result.extractedData?.let { data ->
                    TransactionRepository.saveFromAiData(
                        data,
                        onSuccess = {
                            _infoEvents.value = app.getString(R.string.chat_transaction_saved)
                        },
                        onError = { e ->
                            AppLogger.e("ChatViewModel", "Failed to save AI transaction", e)
                        }
                    )
                }
            } catch (e: Exception) {
                AppLogger.e("ChatViewModel", "AI request failed", e)
                val fallback = ChatMessage(app.getString(R.string.chat_ai_response), false, System.currentTimeMillis())
                db.collection("users").document(uid).collection("chats").add(fallback)
                _errorEvents.value = app.getString(R.string.chat_ai_error)
            } finally {
                _isAiThinking.value = false
            }
        }
    }

    fun saveManualTransaction(
        amountRaw: String,
        type: String,
        category: String,
        note: String
    ) {
        val amount = ValidationUtils.parseAmount(amountRaw)
        if (amount == null) {
            _errorEvents.value = app.getString(R.string.err_invalid_amount)
            return
        }
        if (category.isBlank()) {
            _errorEvents.value = app.getString(R.string.err_category_required)
            return
        }
        _isSavingManual.value = true
        TransactionRepository.saveTransaction(
            amount = amount,
            type = type,
            category = category.trim(),
            note = note.trim(),
            onSuccess = {
                _isSavingManual.value = false
                _infoEvents.value = app.getString(R.string.add_saved_success)
            },
            onError = { e ->
                _isSavingManual.value = false
                _errorEvents.value = app.getString(R.string.add_save_failed, e.message ?: "")
            }
        )
    }

    fun uploadImageAndSend(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        val ts = System.currentTimeMillis()
        val fileRef = storage.reference.child("chat_images").child(uid).child("$ts.jpg")

        _infoEvents.value = app.getString(R.string.chat_uploading)

        fileRef.putFile(uri)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    val imgMsg = ChatMessage("[Image]", true, ts, downloadUrl.toString())
                    db.collection("users").document(uid).collection("chats").add(imgMsg)
                        .addOnSuccessListener { requestAiResponse("[Image uploaded]") }
                }
            }
            .addOnFailureListener { e ->
                _errorEvents.value = app.getString(R.string.chat_upload_failed, e.message ?: "")
            }
    }

    fun clearChatHistory() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("chats").get()
            .addOnSuccessListener { snapshots ->
                val batch = db.batch()
                snapshots.forEach { batch.delete(it.reference) }
                batch.commit().addOnSuccessListener {
                    _infoEvents.value = app.getString(R.string.chat_cleared)
                }
            }
    }

    fun consumeError() { _errorEvents.value = null }
    fun consumeInfo() { _infoEvents.value = null }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
