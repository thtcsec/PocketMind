package com.tuhoang.pocketmind.data.models

data class ChatMessage(
    var content: String = "",
    var isUser: Boolean = false,
    var timestamp: Long = 0L,
    var imageUrl: String? = null
) {
    constructor(content: String, isUser: Boolean, timestamp: Long) : this(
        content = content,
        isUser = isUser,
        timestamp = timestamp,
        imageUrl = null
    )
}
