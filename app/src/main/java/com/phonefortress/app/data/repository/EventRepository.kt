package com.phonefortress.app.data.repository

import com.phonefortress.app.data.local.dao.EventDao
import com.phonefortress.app.data.local.entity.SecurityEventEntity
import com.phonefortress.app.domain.model.EventOperation
import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.domain.model.SecurityEventStatus
import com.phonefortress.app.domain.state.SecurityEventStateMachine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(private val dao: EventDao) {
    private val eventMutex = Mutex()

    suspend fun save(event: SecurityEvent) { dao.upsert(SecurityEventEntity.fromDomain(event)) }

    suspend fun getById(eventId: String): SecurityEvent? = dao.getById(eventId)?.toDomain()

    suspend fun getDispatchable(): List<SecurityEvent> = dao.getDispatchable().map { it.toDomain() }

    suspend fun getActive(): List<SecurityEvent> = dao.getActive().map { it.toDomain() }

    suspend fun getTerminalBefore(timestamp: Long): List<SecurityEvent> =
        dao.getTerminalBefore(timestamp).map { it.toDomain() }

    fun observeRecent(limit: Int = 50): Flow<List<SecurityEvent>> =
        dao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    suspend fun transition(
        eventId: String,
        target: SecurityEventStatus,
        reason: String,
        operation: EventOperation? = null
    ): SecurityEvent? = eventMutex.withLock {
        val current = getById(eventId) ?: return@withLock null
        val next = SecurityEventStateMachine.transitionRequired(
            current, target, reason, operation ?: current.operation
        )
        save(next)
        next
    }

    suspend fun deleteOlderThan(timestamp: Long) { dao.deleteOlderThan(timestamp) }
    suspend fun clearEvidencePathsBefore(timestamp: Long) { dao.clearEvidencePathsBefore(timestamp) }
    suspend fun delete(eventId: String) { dao.deleteById(eventId) }
}
