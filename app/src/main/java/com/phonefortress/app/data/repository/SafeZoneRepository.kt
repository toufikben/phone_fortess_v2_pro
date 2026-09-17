package com.phonefortress.app.data.repository

import com.phonefortress.app.data.local.dao.SafeZoneDao
import com.phonefortress.app.data.local.entity.SafeZoneEntity
import com.phonefortress.app.domain.model.SafeZone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مستودع المناطق الجغرافية.
 */
@Singleton
class SafeZoneRepository @Inject constructor(
    private val dao: SafeZoneDao
) {

    fun observeAll(): Flow<List<SafeZone>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAllEnabled(): List<SafeZone> =
        dao.getAllEnabled().map { it.toDomain() }

    suspend fun getById(id: String): SafeZone? =
        dao.getById(id)?.toDomain()

    suspend fun save(zone: SafeZone) {
        dao.upsert(SafeZoneEntity.fromDomain(zone))
    }

    suspend fun delete(id: String) {
        dao.delete(id)
    }
}
