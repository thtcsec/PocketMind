package com.tuhoang.pocketmind.utils

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object WorkerApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    data class ChatResult(
        val success: Boolean,
        val message: String?,
        val extractedData: Map<String, Any?>?
    )

    fun postChat(workerUrl: String, userId: String, userMessage: String): ChatResult {
        if (workerUrl.isBlank()) {
            return ChatResult(false, null, null)
        }
        val url = workerUrl.trimEnd('/') + "/api/chat"
        val body = JSONObject().apply {
            put("userId", userId)
            put("provider", "openai")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userMessage)
                })
            })
        }
        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody(jsonMedia))
            .build()

        return client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val err = runCatching { JSONObject(text).optString("error") }.getOrNull()
                return ChatResult(false, err ?: "HTTP ${response.code}", null)
            }
            val json = JSONObject(text)
            val success = json.optBoolean("success", false)
            val message = json.optString("message").takeIf { it.isNotEmpty() }
            val dataObj = json.optJSONObject("data")
            val data = dataObj?.let { obj ->
                mapOf(
                    "category" to obj.optString("category"),
                    "amount" to obj.optDouble("amount"),
                    "note" to obj.optString("note"),
                    "type" to obj.optString("type")
                )
            }
            ChatResult(success, message, data)
        }
    }
}
