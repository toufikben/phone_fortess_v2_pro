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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * قناة Webhook عامة — Discord / Slack / Mattermost / مخصص.
 * ترسل JSON، مع توقيع HMAC-SHA256 اختياري.
 */
@Singleton
class WebhookChannel @Inject constructor(
    private val prefs: AlertPrefs,
    private val client: OkHttpClient
) : AlertChannel {

    companion object {
        internal fun isSecureEndpoint(url: String): Boolean =
            url.startsWith("https://", ignoreCase = true)
    }

    override val id: String = "webhook"
    override val displayName: String = "Webhook"
    override val requiresConfig: Boolean = true

    override suspend fun isEnabled(): Boolean = prefs.webhookEnabled.first()

    override suspend fun isConfigured(): Boolean {
        if (!isEnabled()) return false
        return isSecureEndpoint(prefs.webhookUrl.first())
    }

    override suspend fun send(event: SecurityEvent, payload: AlertPayload): AlertResult =
        withContext(Dispatchers.IO) {
            val url = prefs.webhookUrl.first()
            val method = prefs.webhookMethod.first()
            if (url.isBlank()) return@withContext AlertResult.Fatal("Webhook URL missing", id)
            if (!isSecureEndpoint(url)) {
                return@withContext AlertResult.Fatal("Webhook URL must use HTTPS", id)
            }

            try {
                val jsonBody = buildJson(payload)
                val body = jsonBody.toRequestBody("application/json".toMediaType())

                val builder = Request.Builder().url(url).method(method, body)
                builder.header("Content-Type", "application/json")
                builder.header("X-Event-Id", payload.eventId)

                prefs.getWebhookSecret()?.let { secret ->
                    builder.header("X-Signature", hmacSha256(secret, jsonBody))
                }

                client.newCall(builder.build()).execute().use { response ->
                    return@withContext when {
                        response.isSuccessful -> AlertResult.Success("wh-${event.id}", id)
                        response.code in 429..599 -> AlertResult.Retryable("HTTP ${response.code}", id)
                        else -> AlertResult.Fatal("HTTP ${response.code}", id)
                    }
                }
            } catch (e: Exception) {
                Logger.e(e, "Webhook send failed")
                AlertResult.Retryable("Webhook error: ${e.message}", id)
            }
        }

    private fun buildJson(payload: AlertPayload): String {
        val sb = StringBuilder()
        sb.append("{")
        sb.append("\"event_id\":").append(quote(payload.eventId)).append(",")
        sb.append("\"timestamp\":").append(payload.timestamp).append(",")
        sb.append("\"title\":").append(quote(payload.title)).append(",")
        sb.append("\"body\":").append(quote(payload.body)).append(",")
        sb.append("\"has_photo\":").append(payload.photoPath != null).append(",")
        sb.append("\"has_audio\":").append(payload.audioPath != null).append(",")
        if (payload.latitude != null && payload.longitude != null) {
            sb.append("\"latitude\":").append(payload.latitude).append(",")
            sb.append("\"longitude\":").append(payload.longitude).append(",")
        }
        sb.append("\"source\":\"phone_fortress\"")
        sb.append("}")
        return sb.toString()
    }

    private fun quote(s: String): String =
        "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""

    private fun hmacSha256(secret: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        val hash = mac.doFinal(data.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    override suspend fun test(): AlertResult {
        val now = System.currentTimeMillis()
        val testEvent = SecurityEvent(
            id = "test-${now}",
            timestamp = now,
            failedAttempts = 3, threshold = 3, isTest = true,
            threatScore = 50,
            threatLevel = com.phonefortress.app.domain.model.ThreatLevel.MEDIUM
        )
        return send(testEvent, AlertTemplate.build(testEvent))
    }
}
