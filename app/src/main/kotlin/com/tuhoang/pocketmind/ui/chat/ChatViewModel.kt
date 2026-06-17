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
import com.google.android.gms.tasks.Tasks
import com.tuhoang.pocketmind.utils.WorkerApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val PAGE_SIZE = 50L
        private const val MAX_STORED_MESSAGES = 200L
        private const val TRIM_BATCH = 50L
        private const val MAX_AI_CONTEXT_MESSAGES = 20
    }

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

    private val _isLoadingOlder = MutableStateFlow(false)
    val isLoadingOlder: StateFlow<Boolean> = _isLoadingOlder.asStateFlow()

    private val _hasOlderMessages = MutableStateFlow(false)
    val hasOlderMessages: StateFlow<Boolean> = _hasOlderMessages.asStateFlow()

    private var chatListener: ListenerRegistration? = null
    private var oldestLoadedTimestamp: Long? = null

    private fun chatsRef(uid: String) =
        db.collection("users").document(uid).collection("chats")

    fun startListeningForMessages() {
        val uid = auth.currentUser?.uid ?: return

        chatListener?.remove()
        oldestLoadedTimestamp = null
        _hasOlderMessages.value = false

        chatListener = chatsRef(uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    AppLogger.e("ChatViewModel", "Listen failed", error)
                    return@addSnapshotListener
                }
                if (value != null) {
                    val list = value.toObjects(ChatMessage::class.java)
                        .sortedBy { it.timestamp }
                    _messages.value = list
                    oldestLoadedTimestamp = list.firstOrNull()?.timestamp
                    _hasOlderMessages.value = value.size().toLong() >= PAGE_SIZE
                }
            }
    }

    fun loadOlderMessages() {
        val uid = auth.currentUser?.uid ?: return
        val before = oldestLoadedTimestamp ?: return
        if (_isLoadingOlder.value) return

        _isLoadingOlder.value = true
        chatsRef(uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .whereLessThan("timestamp", before)
            .limit(PAGE_SIZE)
            .get()
            .addOnSuccessListener { snapshots ->
                val older = snapshots.toObjects(ChatMessage::class.java)
                if (older.isEmpty()) {
                    _hasOlderMessages.value = false
                } else {
                    val merged = (older + _messages.value).sortedBy { it.timestamp }
                    _messages.value = merged
                    oldestLoadedTimestamp = merged.firstOrNull()?.timestamp
                    _hasOlderMessages.value = snapshots.size().toLong() >= PAGE_SIZE
                }
                _isLoadingOlder.value = false
            }
            .addOnFailureListener { e ->
                AppLogger.e("ChatViewModel", "Load older failed", e)
                _isLoadingOlder.value = false
            }
    }

    fun stopListening() {
        chatListener?.remove()
        chatListener = null
    }

    fun sendMessage(text: String) {
        val uid = auth.currentUser?.uid ?: return
        val userMsg = ChatMessage(text, true, System.currentTimeMillis())
        chatsRef(uid).add(userMsg)
            .addOnSuccessListener {
                trimOldMessagesIfNeeded(uid)
                requestAiResponse(text)
            }
            .addOnFailureListener { e ->
                _errorEvents.value = app.getString(R.string.chat_send_failed, e.message ?: "")
            }
    }

    private fun trimOldMessagesIfNeeded(uid: String) {
        chatsRef(uid).get().addOnSuccessListener { snapshots ->
            val count = snapshots.size()
            if (count <= MAX_STORED_MESSAGES) return@addOnSuccessListener

            val toDelete = snapshots.documents
                .sortedBy { it.getLong("timestamp") ?: 0L }
                .take((count - MAX_STORED_MESSAGES + TRIM_BATCH).toInt().coerceAtLeast(0))
            if (toDelete.isEmpty()) return@addOnSuccessListener

            val batch = db.batch()
            toDelete.forEach { batch.delete(it.reference) }
            batch.commit().addOnSuccessListener {
                AppLogger.d("ChatViewModel", "Trimmed ${toDelete.size} old chat messages")
            }
        }
    }

    private fun requestAiResponse(userText: String) {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val workerUrl = PrefsManager.getInstance().getWorkerUrl()
        _isAiThinking.value = true

        viewModelScope.launch {
            try {
                val contextMessages = _messages.value
                    .takeLast(MAX_AI_CONTEXT_MESSAGES)
                    .joinToString("\n") { msg ->
                        val role = if (msg.isUser) "user" else "assistant"
                        "$role: ${msg.content}"
                    }
                val payload = if (contextMessages.isBlank()) userText else "$contextMessages\nuser: $userText"

                val result = withContext(Dispatchers.IO) {
                    val idToken = Tasks.await(user.getIdToken(false)).token.orEmpty()
                    WorkerApiClient.postChat(workerUrl, idToken, uid, payload)
                }
                val replyText = when {
                    result.message != null -> result.message
                    result.success -> app.getString(R.string.chat_ai_saved)
                    result.error != null -> result.error
                    workerUrl.isBlank() -> app.getString(R.string.chat_ai_response)
                    else -> app.getString(R.string.chat_ai_error)
                }
                val botMsg = ChatMessage(replyText, false, System.currentTimeMillis())
                chatsRef(uid).add(botMsg).addOnSuccessListener { trimOldMessagesIfNeeded(uid) }

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
                chatsRef(uid).add(fallback)
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
        note: String,
        receiptUri: Uri? = null
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

        fun persist(receiptUrl: String?) {
            TransactionRepository.saveTransaction(
                amount = amount,
                type = type,
                category = category.trim(),
                note = note.trim(),
                receiptUrl = receiptUrl,
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

        val uid = auth.currentUser?.uid
        if (uid == null) {
            _isSavingManual.value = false
            _errorEvents.value = app.getString(R.string.add_save_failed, "Not logged in")
            return
        }

        if (receiptUri == null) {
            persist(null)
            return
        }

        val ts = System.currentTimeMillis()
        val fileRef = storage.reference.child("receipts").child(uid).child("$ts.jpg")
        fileRef.putFile(receiptUri)
            .addOnSuccessListener {
                fileRef.downloadUrl
                    .addOnSuccessListener { url -> persist(url.toString()) }
                    .addOnFailureListener { e ->
                        _isSavingManual.value = false
                        _errorEvents.value = app.getString(R.string.add_save_failed, e.message ?: "")
                    }
            }
            .addOnFailureListener { e ->
                _isSavingManual.value = false
                _errorEvents.value = app.getString(R.string.add_save_failed, e.message ?: "")
            }
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
                    chatsRef(uid).add(imgMsg)
                        .addOnSuccessListener {
                            trimOldMessagesIfNeeded(uid)
                            requestAiResponse("[Image uploaded]")
                        }
                }
            }
            .addOnFailureListener { e ->
                _errorEvents.value = app.getString(R.string.chat_upload_failed, e.message ?: "")
            }
    }

    fun clearChatHistory(onComplete: () -> Unit = {}) {
        val uid = auth.currentUser?.uid ?: return
        chatsRef(uid).get()
            .addOnSuccessListener { snapshots ->
                val batch = db.batch()
                snapshots.forEach { batch.delete(it.reference) }
                batch.commit().addOnSuccessListener {
                    _messages.value = emptyList()
                    _hasOlderMessages.value = false
                    oldestLoadedTimestamp = null
                    _infoEvents.value = app.getString(R.string.chat_cleared)
                    onComplete()
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
