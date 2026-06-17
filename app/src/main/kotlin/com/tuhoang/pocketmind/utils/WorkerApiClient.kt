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
        val extractedData: Map<String, Any?>?,
        val error: String? = null
    )

    fun postChat(workerUrl: String, idToken: String, userId: String, userMessage: String): ChatResult {
        if (workerUrl.isBlank()) {
            return ChatResult(false, null, null, "Worker URL not configured")
        }
        if (idToken.isBlank()) {
            return ChatResult(false, null, null, "Not authenticated")
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
            .addHeader("Authorization", "Bearer $idToken")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()

        return client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val err = runCatching {
                    JSONObject(text).optString("error").takeIf { it.isNotEmpty() }
                }.getOrNull()
                return ChatResult(false, null, null, err ?: "HTTP ${response.code}")
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
