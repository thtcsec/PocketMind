package com.tuhoang.pocketmind.ui.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.tuhoang.pocketmind.data.models.ChatMessage
import com.tuhoang.pocketmind.utils.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _errorEvents = MutableStateFlow<String?>(null)
    val errorEvents: StateFlow<String?> = _errorEvents.asStateFlow()

    private val _infoEvents = MutableStateFlow<String?>(null)
    val infoEvents: StateFlow<String?> = _infoEvents.asStateFlow()

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
            .addOnSuccessListener { simulateAiResponse() }
            .addOnFailureListener { e ->
                _errorEvents.value = "Failed to send message: ${e.message}"
            }
    }

    private fun simulateAiResponse() {
        val uid = auth.currentUser?.uid ?: return
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val botMsg = ChatMessage(
                "I will categorize your last transaction automatically.",
                false,
                System.currentTimeMillis()
            )
            db.collection("users").document(uid).collection("chats").add(botMsg)
        }, 1000)
    }

    fun uploadImageAndSend(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        val ts = System.currentTimeMillis()
        val fileRef = storage.reference.child("chat_images").child(uid).child("$ts.jpg")

        _infoEvents.value = "Uploading image..."

        fileRef.putFile(uri)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    val imgMsg = ChatMessage("[Image]", true, ts, downloadUrl.toString())
                    db.collection("users").document(uid).collection("chats").add(imgMsg)
                        .addOnSuccessListener { simulateAiResponse() }
                }
            }
            .addOnFailureListener { e ->
                _errorEvents.value = "Upload failed: ${e.message}"
            }
    }

    fun clearChatHistory() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("chats").get()
            .addOnSuccessListener { snapshots ->
                val batch = db.batch()
                snapshots.forEach { batch.delete(it.reference) }
                batch.commit().addOnSuccessListener {
                    _infoEvents.value = "Chat history cleared from cloud."
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
