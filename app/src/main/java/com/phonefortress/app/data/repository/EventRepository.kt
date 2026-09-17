package com.phonefortress.app.data.repository

import com.phonefortress.app.data.local.dao.EventDao
import com.phonefortress.app.data.local.entity.SecurityEventEntity
import com.phonefortress.app.domain.model.EventOperation
import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.domain.model.SecurityEventStatus
import com.phonefortress.app.domain.state.SecurityEventStateMachine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

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
    suspend fun getDispatchable(): List<SecurityEvent> = dao.getDispatchable().map { it.toDomain() }
    suspend fun getActive(): List<SecurityEvent> = dao.getActive().map { it.toDomain() }
    suspend fun getStaleInProgress(before: Long): List<SecurityEvent> = dao.getStaleInProgress(before).map { it.toDomain() }
    suspend fun getTerminalBefore(timestamp: Long): List<SecurityEvent> = dao.getTerminalBefore(timestamp).map { it.toDomain() }
    suspend fun claimForSend(eventId: String, now: Long = System.currentTimeMillis()): Boolean =
        dao.claimSend(eventId, now, now - SEND_CLAIM_LEASE_MS) == 1
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
