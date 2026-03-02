package com.tuhoang.pocketmind.data.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_messages")
public class ChatMessage {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String content;
    public boolean isUser;
    public long timestamp;
    public String imageUrl;

    // Required by Firestore
    public ChatMessage() {
    }

    public ChatMessage(String content, boolean isUser, long timestamp) {
        this.content = content;
        this.isUser = isUser;
        this.timestamp = timestamp;
        this.imageUrl = null;
    }
    
    public ChatMessage(String content, boolean isUser, long timestamp, String imageUrl) {
        this.content = content;
        this.isUser = isUser;
        this.timestamp = timestamp;
        this.imageUrl = imageUrl;
    }
}
