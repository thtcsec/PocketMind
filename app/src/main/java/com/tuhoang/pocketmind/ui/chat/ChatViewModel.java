package com.tuhoang.pocketmind.ui.chat;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.tuhoang.pocketmind.data.models.ChatMessage;
import com.tuhoang.pocketmind.utils.AppLogger;

import java.util.List;

public class ChatViewModel extends ViewModel {

    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>();
    private final MutableLiveData<String> errorEvents = new MutableLiveData<>();
    private final MutableLiveData<String> infoEvents = new MutableLiveData<>();
    
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    
    private ListenerRegistration chatListener;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public LiveData<List<ChatMessage>> getMessages() {
        return messages;
    }

    public LiveData<String> getErrorEvents() {
        return errorEvents;
    }

    public LiveData<String> getInfoEvents() {
        return infoEvents;
    }

    public void startListeningForMessages() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        if (chatListener != null) {
            chatListener.remove();
        }

        chatListener = db.collection("users").document(uid).collection("chats")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        AppLogger.e("ChatViewModel", "Listen failed", error);
                        return;
                    }

                    if (value != null) {
                        List<ChatMessage> history = value.toObjects(ChatMessage.class);
                        messages.setValue(history);
                    }
                });
    }

    public void stopListening() {
        if (chatListener != null) {
            chatListener.remove();
            chatListener = null;
        }
    }

    public void sendMessage(String text) {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        ChatMessage userMsg = new ChatMessage(text, true, System.currentTimeMillis());
        db.collection("users").document(uid).collection("chats").add(userMsg)
                .addOnSuccessListener(documentReference -> simulateAiResponse())
                .addOnFailureListener(e -> errorEvents.setValue("Failed to send message: " + e.getMessage()));
    }

    private void simulateAiResponse() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        mainThreadHandler.postDelayed(() -> {
            ChatMessage botMsg = new ChatMessage("I will categorize your last transaction automatically.", false, System.currentTimeMillis());
            db.collection("users").document(uid).collection("chats").add(botMsg);
        }, 1000);
    }

    public void uploadImageAndSend(Uri uri) {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        
        long ts = System.currentTimeMillis();
        StorageReference fileRef = storage.getReference().child("chat_images").child(uid).child(ts + ".jpg");

        infoEvents.setValue("Uploading image...");
        
        fileRef.putFile(uri).addOnSuccessListener(taskSnapshot -> {
            fileRef.getDownloadUrl().addOnSuccessListener(downloadUrl -> {
                ChatMessage imgMsg = new ChatMessage("[Image]", true, ts, downloadUrl.toString());
                db.collection("users").document(uid).collection("chats").add(imgMsg)
                    .addOnSuccessListener(documentReference -> simulateAiResponse());
            });
        }).addOnFailureListener(e -> {
            errorEvents.setValue("Upload failed: " + e.getMessage());
        });
    }

    public void clearChatHistory() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).collection("chats").get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                WriteBatch batch = db.batch();
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    batch.delete(doc.getReference());
                }
                batch.commit().addOnSuccessListener(aVoid -> {
                    infoEvents.setValue("Chat history cleared from cloud.");
                });
            });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopListening();
    }
}
