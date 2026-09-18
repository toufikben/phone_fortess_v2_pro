package com.phonefortress.app.data.repository

import com.phonefortress.app.data.local.dao.EventDao
import com.phonefortress.app.data.local.entity.SecurityEventEntity
import com.phonefortress.app.domain.model.EventOperation
import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.domain.model.SecurityEventStatus
import com.phonefortress.app.domain.state.SecurityEventStateMachine
import com.phonefortress.app.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

@Singleton
class EventRepository @Inject constructor(private val dao: EventDao) {
    suspend fun create(event: SecurityEvent) {
        require(event.status == SecurityEventStatus.PENDING) { "New events must start in PENDING" }
        dao.insert(SecurityEventEntity.fromDomain(event))
    }

    suspend fun updateMetadata(event: SecurityEvent) {
        dao.updateMetadata(
            eventId = event.id,
            photoPath = event.photoPath,
            audioPath = event.audioPath,
            latitude = event.latitude,
            longitude = event.longitude,
            locationAccuracy = event.locationAccuracy,
            threatScore = event.threatScore,
            threatLevel = event.threatLevel.name,
            threatReasons = event.threatReasons.joinToString("|||")
        )
    }

    suspend fun getById(eventId: String): SecurityEvent? = dao.getById(eventId)?.toDomain()
    suspend fun getDispatchable(limit: Int = Constants.MAX_EVENT_BATCH_SIZE): List<SecurityEvent> =
        dao.getDispatchable(limit.coerceIn(1, Constants.MAX_EVENT_BATCH_SIZE)).map { it.toDomain() }
    suspend fun getActive(limit: Int = Constants.MAX_EVENT_BATCH_SIZE): List<SecurityEvent> =
        dao.getActive(limit.coerceIn(1, Constants.MAX_EVENT_BATCH_SIZE)).map { it.toDomain() }
    suspend fun getStaleInProgress(before: Long, limit: Int = Constants.MAX_EVENT_BATCH_SIZE): List<SecurityEvent> =
        dao.getStaleInProgress(before, limit.coerceIn(1, Constants.MAX_EVENT_BATCH_SIZE)).map { it.toDomain() }
    suspend fun getTerminalBefore(timestamp: Long, limit: Int = Constants.MAX_EVENT_BATCH_SIZE): List<SecurityEvent> =
        dao.getTerminalBefore(timestamp, limit.coerceIn(1, Constants.MAX_EVENT_BATCH_SIZE)).map { it.toDomain() }
    suspend fun claimCapture(eventId: String, now: Long = System.currentTimeMillis()): String? {
        val attempt = UUID.randomUUID().toString()
        return attempt.takeIf { dao.claimCapture(eventId, it, now) == 1 }
    }
    suspend fun claimForSend(eventId: String, now: Long = System.currentTimeMillis()): String? {
        val owner = UUID.randomUUID().toString()
        return owner.takeIf { dao.claimSend(eventId, it, now, now - SEND_CLAIM_LEASE_MS) == 1 }
    }
    suspend fun transitionCaptureOwned(eventId: String, attemptId: String, target: SecurityEventStatus, reason: String): Boolean =
        dao.transitionCapture(eventId, attemptId, target.name, EventOperation.CAPTURE.name, reason, System.currentTimeMillis()) == 1
    suspend fun transitionSendOwned(eventId: String, expected: SecurityEventStatus, target: SecurityEventStatus, owner: String, reason: String): Boolean =
        dao.transitionSendOwned(eventId, expected.name, target.name, owner, reason, System.currentTimeMillis()) == 1
    fun observeRecent(limit: Int = 50): Flow<List<SecurityEvent>> = dao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    /** State validation happens in-process; the conditional UPDATE is the cross-process claim. */
    suspend fun transition(
        eventId: String,
        target: SecurityEventStatus,
        reason: String,
        operation: EventOperation? = null
    ): SecurityEvent? {
        val current = getById(eventId) ?: return null
        val effectiveOperation = operation ?: current.operation
        val next = SecurityEventStateMachine.transitionRequired(current, target, reason, effectiveOperation)
        val updated = dao.transitionStatus(
            eventId = eventId,
            expectedStatus = current.status.name,
            targetStatus = next.status.name,
            operation = next.operation.name,
            reason = reason,
            transitionAt = System.currentTimeMillis()
        )
        return if (updated == 1) getById(eventId) else null
    }

    suspend fun deleteOlderThan(timestamp: Long) { dao.deleteOlderThan(timestamp) }
    suspend fun clearEvidencePathsBefore(timestamp: Long) { dao.clearEvidencePathsBefore(timestamp) }
    suspend fun clearEvidencePaths(eventId: String) { dao.clearEvidencePaths(eventId) }
    suspend fun delete(eventId: String) { dao.deleteById(eventId) }

    private companion object { const val SEND_CLAIM_LEASE_MS = 5 * 60_000L }
}
