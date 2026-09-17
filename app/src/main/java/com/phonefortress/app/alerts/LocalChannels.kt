package com.phonefortress.app.alerts

import android.content.Context
import android.telephony.SmsManager
import com.phonefortress.app.domain.model.*

class SmsChannel(private val context: Context, private val recipients: List<String>) : AlertChannel {
    override val id = "sms"; override val displayName = "SMS"; override val requiresConfig = true
    override suspend fun isConfigured() = recipients.isNotEmpty()
    override suspend fun send(event: SecurityEvent, payload: AlertPayload): AlertResult = runCatching {
        val manager = SmsManager.getDefault()
        recipients.forEach { manager.sendTextMessage(it, null, payload.message.take(160), null, null) }
        AlertResult.Success("sms")
    }.getOrElse { AlertResult.Fatal("sms_failed") }
}

class GenericSmtpChannel(private val host: String?, private val port: Int, private val username: String?, private val password: String?) : AlertChannel {
    override val id = "smtp"; override val displayName = "SMTP"; override val requiresConfig = true
    override suspend fun isConfigured() = !host.isNullOrBlank() && !username.isNullOrBlank() && !password.isNullOrBlank()
    override suspend fun send(event: SecurityEvent, payload: AlertPayload): AlertResult = AlertResult.Retryable("smtp_adapter_pending")
}

class LocalNotificationChannel : AlertChannel {
    override val id = "local"; override val displayName = "Local notification"; override val requiresConfig = false
    override suspend fun isConfigured() = true
    override suspend fun send(event: SecurityEvent, payload: AlertPayload) = AlertResult.Success("local")
}
