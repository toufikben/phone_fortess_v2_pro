package com.phonefortress.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.phonefortress.app.domain.model.EventOperation
import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.domain.model.SecurityEventStatus
import com.phonefortress.app.domain.model.ThreatLevel

@Entity(
    tableName = "security_events",
    indices = [Index("timestamp"), Index("status"), Index(value = ["eventId"], unique = true)]
)
data class SecurityEventEntity(
    @PrimaryKey val eventId: String,
    val timestamp: Long,
    val failedAttempts: Int,
    val threshold: Int,
    val photoPath: String?,
    val audioPath: String?,
    val latitude: Double?,
    val longitude: Double?,
    val locationAccuracy: Float?,
    val threatScore: Int,
    val threatLevel: String,
    val threatReasons: String,
    val status: String,
    val operation: String,
    val lastTransitionReason: String?,
    val isTest: Boolean
) {
    fun toDomain() = SecurityEvent(
        id = eventId, timestamp = timestamp, failedAttempts = failedAttempts, threshold = threshold,
        photoPath = photoPath, audioPath = audioPath, latitude = latitude, longitude = longitude,
        locationAccuracy = locationAccuracy, threatScore = threatScore,
        threatLevel = runCatching { ThreatLevel.valueOf(threatLevel) }.getOrDefault(ThreatLevel.LOW),
        threatReasons = threatReasons.split("|||").filter(String::isNotBlank),
        status = runCatching { SecurityEventStatus.valueOf(status) }.getOrDefault(SecurityEventStatus.PENDING),
        operation = runCatching { EventOperation.valueOf(operation) }.getOrDefault(EventOperation.CAPTURE),
        lastTransitionReason = lastTransitionReason, isTest = isTest
    )

    companion object {
        fun fromDomain(e: SecurityEvent) = SecurityEventEntity(
            eventId = e.id, timestamp = e.timestamp, failedAttempts = e.failedAttempts,
            threshold = e.threshold, photoPath = e.photoPath, audioPath = e.audioPath,
            latitude = e.latitude, longitude = e.longitude, locationAccuracy = e.locationAccuracy,
            threatScore = e.threatScore, threatLevel = e.threatLevel.name,
            threatReasons = e.threatReasons.joinToString("|||"), status = e.status.name,
            operation = e.operation.name, lastTransitionReason = e.lastTransitionReason, isTest = e.isTest
        )
    }
}
