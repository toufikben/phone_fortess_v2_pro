package com.phonefortress.app.alerts.channels

import com.phonefortress.app.alerts.template.AlertTemplate
import com.phonefortress.app.data.prefs.AlertPrefs
import com.phonefortress.app.domain.model.AlertChannel
import com.phonefortress.app.domain.model.AlertPayload
import com.phonefortress.app.domain.model.AlertResult
import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * قناة Telegram Bot API — إرسال مباشر من الجهاز.
 */
@Singleton
class TelegramChannel @Inject constructor(
    private val prefs: AlertPrefs,
    private val client: OkHttpClient,
    private val json: Json
) : AlertChannel {

    override val id: String = "telegram"
    override val displayName: String = "Telegram"
    override val requiresConfig: Boolean = true

    override suspend fun isEnabled(): Boolean = prefs.telegramEnabled.first()

    override suspend fun isConfigured(): Boolean {
        if (!isEnabled()) return false
        val token = prefs.getTelegramToken() ?: return false
        val chatId = prefs.telegramChatId.first()
        return token.isNotBlank() && chatId.isNotBlank()
    }

    override suspend fun send(event: SecurityEvent, payload: AlertPayload): AlertResult =
        withContext(Dispatchers.IO) {
            val token = prefs.getTelegramToken()
                ?: return@withContext AlertResult.Fatal("Telegram token missing", id)
            val chatId = prefs.telegramChatId.first()
            if (chatId.isBlank()) return@withContext AlertResult.Fatal("Chat ID missing", id)

            try {
                if (payload.photoPath != null && File(payload.photoPath).exists()) {
                    sendPhoto(token, chatId, payload)
                } else {
                    sendMessage(token, chatId, payload.body)
                }
            } catch (e: Exception) {
                Logger.e(e, "Telegram send failed")
                AlertResult.Retryable("Telegram error: ${e.message}", id)
            }
        }

    private fun sendMessage(token: String, chatId: String, text: String): AlertResult {
        val url = "https://api.telegram.org/bot$token/sendMessage"
        val body = buildJson(chatId, text).toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).execute().use { response ->
            return if (response.isSuccessful) {
                val responseBody = response.body?.string().orEmpty()
                val msgId = parseMessageId(responseBody) ?: "unknown"
                AlertResult.Success(msgId, id)
            } else if (response.code in 500..599) {
                AlertResult.Retryable("Telegram HTTP ${response.code}", id)
            } else {
                AlertResult.Fatal("Telegram HTTP ${response.code}", id)
            }
        }
    }

    private fun sendPhoto(token: String, chatId: String, payload: AlertPayload): AlertResult {
        val url = "https://api.telegram.org/bot$token/sendPhoto"
        val file = File(payload.photoPath!!)
        val photoBody = file.asRequestBody("image/jpeg".toMediaType())
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart("caption", payload.body)
            .addFormDataPart("photo", "intruder.jpg", photoBody)
            .build()
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).execute().use { response ->
            return if (response.isSuccessful) {
                val responseBody = response.body?.string().orEmpty()
                val msgId = parseMessageId(responseBody) ?: "unknown"
                AlertResult.Success(msgId, id)
            } else if (response.code in 500..599) {
                AlertResult.Retryable("Telegram HTTP ${response.code}", id)
            } else {
                AlertResult.Fatal("Telegram HTTP ${response.code}", id)
            }
        }
    }

    private fun buildJson(chatId: String, text: String): String {
        val escaped = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        return """{"chat_id":"$chatId","text":"$escaped"}"""
    }

    private fun parseMessageId(body: String): String? = runCatching {
        json.parseToJsonElement(body).jsonObject["result"]?.jsonObject?.get("message_id")?.jsonPrimitive?.content
    }.getOrNull()

    override suspend fun test(): AlertResult {
        val now = System.currentTimeMillis()
        val testEvent = SecurityEvent(
            id = "test-${now}",
            timestamp = now,
            failedAttempts = 3,
            threshold = 3,
            isTest = true,
            threatScore = 50,
            threatLevel = com.phonefortress.app.domain.model.ThreatLevel.MEDIUM
        )
        return send(testEvent, AlertTemplate.build(testEvent))
    }
}
