package com.phonefortress.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SecurityEvent(
    val id: String,
    val timestamp: Long,
    val failedAttempts: Int,
    val threshold: Int,
    val photoPath: String? = null,
    val audioPath: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAccuracy: Float? = null,
    val threatScore: Int = 0,
    val threatLevel: ThreatLevel = ThreatLevel.LOW,
    val threatReasons: List<String> = emptyList(),
    val status: SecurityEventStatus = SecurityEventStatus.PENDING,
    val operation: EventOperation = EventOperation.CAPTURE,
    val lastTransitionReason: String? = null,
    val retryCount: Int = 0,
    val lastTransitionAt: Long = timestamp,
    val sendClaimedAt: Long? = null,
    val isTest: Boolean = false
)

@Serializable
enum class SecurityEventStatus {
    PENDING, DEFERRED, IN_PROGRESS, CAPTURED,
    SEND_PENDING, SENT, FAILED_RETRYABLE, FAILED_FINAL, CANCELLED
}

@Serializable
enum class EventOperation { CAPTURE, SEND }

@Serializable
enum class ThreatLevel {
    LOW, MEDIUM, HIGH, CRITICAL;
    val emoji: String
        get() = when (this) {
            LOW -> "🟢"
            MEDIUM -> "🟡"
            HIGH -> "🟠"
            CRITICAL -> "🔴"
        }
}

@Serializable
data class AlertPayload(
    val title: String,
    val body: String,
    val photoPath: String? = null,
    val audioPath: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val eventId: String,
    val timestamp: Long
)

sealed class AlertResult {
    data class Success(val messageId: String, val channelId: String) : AlertResult()
    data class Retryable(val reason: String, val channelId: String) : AlertResult()
    data class Fatal(val reason: String, val channelId: String) : AlertResult()
    data class NotConfigured(val channelId: String) : AlertResult()
}
