package com.phonefortress.app.alerts

import com.phonefortress.app.domain.model.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class TelegramChannel(private val token: String?, private val chatId: String?, private val client: OkHttpClient = OkHttpClient()) : AlertChannel {
    override val id = "telegram"; override val displayName = "Telegram"; override val requiresConfig = true
    override suspend fun isConfigured() = !token.isNullOrBlank() && !chatId.isNullOrBlank()
    override suspend fun send(event: SecurityEvent, payload: AlertPayload): AlertResult {
        val body = "chat_id=${chatId}&text=${payload.message}".toRequestBody("application/x-www-form-urlencoded".toMediaType())
        val response = client.newCall(Request.Builder().url("https://api.telegram.org/bot$token/sendMessage").post(body).build()).execute()
        response.use { return if (it.isSuccessful) AlertResult.Success("telegram") else AlertResult.Retryable("http_${it.code}") }
    }
}

class NtfyChannel(private val topicUrl: String?, private val client: OkHttpClient = OkHttpClient()) : AlertChannel {
    override val id = "ntfy"; override val displayName = "ntfy"; override val requiresConfig = true
    override suspend fun isConfigured() = !topicUrl.isNullOrBlank()
    override suspend fun send(event: SecurityEvent, payload: AlertPayload): AlertResult {
        val body = payload.message.toRequestBody("text/plain".toMediaType())
        val response = client.newCall(Request.Builder().url(topicUrl!!).post(body).build()).execute()
        response.use { return if (it.isSuccessful) AlertResult.Success("ntfy") else AlertResult.Retryable("http_${it.code}") }
    }
}

class WebhookChannel(private val url: String?, private val secret: String?, private val client: OkHttpClient = OkHttpClient()) : AlertChannel {
    override val id = "webhook"; override val displayName = "Webhook"; override val requiresConfig = true
    override suspend fun isConfigured() = !url.isNullOrBlank()
    override suspend fun send(event: SecurityEvent, payload: AlertPayload): AlertResult {
        val json = "{\"title\":\"${payload.title}\",\"message\":\"${payload.message}\",\"eventId\":\"${event.id}\"}"
        val request = Request.Builder().url(url!!).post(json.toRequestBody("application/json".toMediaType())).apply { secret?.let { addHeader("X-Phone-Fortress-Secret", it) } }.build()
        val response = client.newCall(request).execute()
        response.use { return if (it.isSuccessful) AlertResult.Success("webhook") else AlertResult.Retryable("http_${it.code}") }
    }
}
