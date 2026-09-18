package com.phonefortress.app.alerts.channels

import android.content.Context
import android.telephony.SmsManager
import com.phonefortress.app.alerts.template.AlertTemplate
import com.phonefortress.app.data.prefs.AlertPrefs
import com.phonefortress.app.domain.model.AlertChannel
import com.phonefortress.app.domain.model.AlertPayload
import com.phonefortress.app.domain.model.AlertResult
import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * قناة SMS — ترسل نصاً قصيراً للأرقام الموثوقة.
 * ملاحظة: لا يُرفق صورة أو صوت (قيود SMS).
 */
@Singleton
class SmsChannel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: AlertPrefs
) : AlertChannel {

    private companion object { const val MAX_RECIPIENTS = 10 }

    override val id: String = "sms"
    override val displayName: String = "SMS"
    override val requiresConfig: Boolean = true

    override suspend fun isEnabled(): Boolean = prefs.smsEnabled.first()

    override suspend fun isConfigured(): Boolean {
        if (!isEnabled()) return false
        return prefs.smsNumbers.first().isNotBlank()
    }

    override suspend fun send(event: SecurityEvent, payload: AlertPayload): AlertResult =
        withContext(Dispatchers.IO) {
            val numbers = prefs.smsNumbers.first()
                .split(",", ";")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()

            if (numbers.isEmpty()) return@withContext AlertResult.Fatal("No SMS numbers", id)
            if (numbers.size > MAX_RECIPIENTS) {
                return@withContext AlertResult.Fatal("Too many SMS recipients", id)
            }

            val shortBody = buildShortBody(payload)

            return@withContext try {
                val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }

                numbers.forEach { number ->
                    val parts = smsManager.divideMessage(shortBody)
                    smsManager.sendMultipartTextMessage(number, null, parts, null, null)
                }
                AlertResult.Success("sms-${event.id}", id)
            } catch (e: SecurityException) {
                Logger.e(e, "SMS permission denied")
                AlertResult.Fatal("SMS permission denied", id)
            } catch (e: Exception) {
                Logger.e(e, "SMS send failed")
                AlertResult.Retryable("SMS error: ${e.message}", id)
            }
        }

    private fun buildShortBody(payload: AlertPayload): String {
        return buildString {
            appendLine("🛡️ Phone Fortress")
            appendLine(payload.title)
            if (payload.latitude != null && payload.longitude != null) {
                appendLine("📍 https://maps.google.com/?q=${payload.latitude},${payload.longitude}")
            }
            appendLine("ID: ${payload.eventId.take(8)}")
        }.trim()
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
