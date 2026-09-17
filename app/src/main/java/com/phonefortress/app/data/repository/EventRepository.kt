package com.phonefortress.app.data.repository

import com.phonefortress.app.data.local.dao.EventDao
import com.phonefortress.app.data.local.entity.SecurityEventEntity
import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.domain.model.SecurityEventStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مستودع الأحداث الأمنية.
 */
@Singleton
class EventRepository @Inject constructor(
    private val dao: EventDao
) {

    suspend fun save(event: SecurityEvent) {
        dao.upsert(SecurityEventEntity.fromDomain(event))
    }

    suspend fun getById(eventId: String): SecurityEvent? =
        dao.getById(eventId)?.toDomain()

    suspend fun getActive(): List<SecurityEvent> =
        dao.getActive().map { it.toDomain() }

    fun observeRecent(limit: Int = 50): Flow<List<SecurityEvent>> =
        dao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    suspend fun updateStatus(eventId: String, status: SecurityEventStatus) {
        dao.updateStatus(eventId, status.name)
    }

    suspend fun deleteOlderThan(timestamp: Long) {
        dao.deleteOlderThan(timestamp)
    }

    suspend fun clearEvidencePathsBefore(timestamp: Long) {
        dao.clearEvidencePathsBefore(timestamp)
    }

    suspend fun delete(eventId: String) {
        dao.deleteById(eventId)
    }
}
