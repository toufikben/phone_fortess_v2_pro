package com.phonefortress.app.domain.usecase

import android.content.Context
import com.phonefortress.app.data.prefs.SecurityPrefs
import com.phonefortress.app.data.repository.EventRepository
import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.domain.model.SecurityEventStatus
import com.phonefortress.app.domain.model.ThreatLevel
import com.phonefortress.app.geofence.ZoneStateHolder
import com.phonefortress.app.platform.service.CameraForegroundService
import com.phonefortress.app.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaptureEvidenceUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eventRepository: EventRepository,
    private val securityPrefs: SecurityPrefs,
    private val zoneState: ZoneStateHolder
) {
    suspend fun start(attempts: Int, isTest: Boolean = false): String {
        val configuredThreshold = securityPrefs.threshold.first()
        val effectiveThreshold = zoneState.effectiveThreshold(configuredThreshold)
        val eventId = UUID.randomUUID().toString()
        val event = SecurityEvent(
            id = eventId,
            timestamp = System.currentTimeMillis(),
            failedAttempts = attempts,
            threshold = effectiveThreshold,
            status = SecurityEventStatus.PENDING,
            threatScore = computeInitialThreatScore(attempts, effectiveThreshold),
            threatLevel = ThreatLevel.LOW,
            lastTransitionReason = "event-created-effective-threshold=$effectiveThreshold;configured=$configuredThreshold",
            isTest = isTest
        )
        eventRepository.create(event)
        Logger.i("Event created: $eventId (attempts=$attempts, threshold=$effectiveThreshold, test=$isTest)")
        CameraForegroundService.start(context, eventId, isTest)
        return eventId
    }

    private fun computeInitialThreatScore(attempts: Int, threshold: Int): Int {
        val ratio = if (threshold > 0) attempts.toFloat() / threshold else 0f
        return when {
            ratio >= 3f -> 80
            ratio >= 2f -> 60
            ratio >= 1.5f -> 40
            else -> 20
        }.coerceIn(0, 100)
    }
}
