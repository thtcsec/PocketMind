package com.tuhoang.pocketmind.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.tuhoang.pocketmind.data.models.ChatMessage;

import java.util.List;

@Dao
public interface ChatDao {

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    List<ChatMessage> getAllMessages();

    @Insert
    void insert(ChatMessage message);

    @Query("DELETE FROM chat_messages")
    void deleteAll();
}
