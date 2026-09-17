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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * قناة ntfy.sh — إشعارات فورية بدون تسجيل.
 * المستخدم يُنشئ topic سري على ntfy.sh ويضعه هنا.
 */
@Singleton
class NtfyChannel @Inject constructor(
    private val prefs: AlertPrefs,
    private val client: OkHttpClient
) : AlertChannel {

    override val id: String = "ntfy"
    override val displayName: String = "ntfy.sh"
    override val requiresConfig: Boolean = true

    override suspend fun isEnabled(): Boolean = prefs.ntfyEnabled.first()

    override suspend fun isConfigured(): Boolean {
        if (!isEnabled()) return false
        val url = prefs.ntfyUrl.first()
        return url.startsWith("https://", ignoreCase = true)
    }

    override suspend fun send(event: SecurityEvent, payload: AlertPayload): AlertResult =
        withContext(Dispatchers.IO) {
            val url = prefs.ntfyUrl.first()
            if (url.isBlank()) return@withContext AlertResult.Fatal("ntfy URL missing", id)
            if (!url.startsWith("https://", ignoreCase = true)) {
                return@withContext AlertResult.Fatal("ntfy URL must use HTTPS", id)
            }

            try {
                val requestBuilder = Request.Builder()
                    .url(url)
                    .post(payload.body.toRequestBody("text/plain".toMediaType()))
                    .header("Title", payload.title)
                    .header("Priority", priorityFor(event))
                    .header("Tags", tagsFor(event))

                prefs.getNtfyAuth()?.let { auth ->
                    requestBuilder.header("Authorization", auth)
                }

                client.newCall(requestBuilder.build()).execute().use { response ->
                    return@withContext when {
                        response.isSuccessful -> AlertResult.Success("ntfy-${event.id}", id)
                        response.code in 500..599 -> AlertResult.Retryable("HTTP ${response.code}", id)
                        else -> AlertResult.Fatal("HTTP ${response.code}", id)
                    }
                }
            } catch (e: Exception) {
                Logger.e(e, "ntfy send failed")
                AlertResult.Retryable("ntfy error: ${e.message}", id)
            }
        }

    private fun priorityFor(event: SecurityEvent) = when {
        event.isTest -> "default"
        event.threatScore >= 80 -> "urgent"
        event.threatScore >= 60 -> "high"
        event.threatScore >= 30 -> "default"
        else -> "low"
    }

    private fun tagsFor(event: SecurityEvent) = buildList {
        add("shield")
        if (event.photoPath != null) add("camera")
        if (event.latitude != null) add("round_pushpin")
        when {
            event.threatScore >= 80 -> add("rotating_light")
            event.threatScore >= 60 -> add("warning")
        }
    }.joinToString(",")

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
