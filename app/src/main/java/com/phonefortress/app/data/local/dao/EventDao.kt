package com.phonefortress.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.phonefortress.app.data.local.entity.SecurityEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: SecurityEventEntity)

    @Query("""
        UPDATE security_events SET status = :targetStatus, operation = :operation,
            lastTransitionReason = :reason
        WHERE eventId = :eventId AND status = :expectedStatus
    """)
    suspend fun transitionStatus(
        eventId: String,
        expectedStatus: String,
        targetStatus: String,
        operation: String,
        reason: String
    ): Int

    @Query("""
        UPDATE security_events SET
            photoPath = :photoPath,
            audioPath = :audioPath,
            latitude = :latitude,
            longitude = :longitude,
            locationAccuracy = :locationAccuracy,
            threatScore = :threatScore,
            threatLevel = :threatLevel,
            threatReasons = :threatReasons
        WHERE eventId = :eventId
    """)
    suspend fun updateMetadata(
        eventId: String,
        photoPath: String?,
        audioPath: String?,
        latitude: Double?,
        longitude: Double?,
        locationAccuracy: Float?,
        threatScore: Int,
        threatLevel: String,
        threatReasons: String
    )

    @Query("SELECT * FROM security_events WHERE eventId = :eventId LIMIT 1")
    suspend fun getById(eventId: String): SecurityEventEntity?

    @Query("SELECT * FROM security_events ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<SecurityEventEntity>>

    @Query("SELECT * FROM security_events WHERE status IN ('CAPTURED','SEND_PENDING','FAILED_RETRYABLE') ORDER BY timestamp ASC")
    suspend fun getDispatchable(): List<SecurityEventEntity>

    @Query("SELECT * FROM security_events WHERE status IN ('PENDING','DEFERRED','IN_PROGRESS','CAPTURED','SEND_PENDING','FAILED_RETRYABLE') ORDER BY timestamp ASC")
    suspend fun getActive(): List<SecurityEventEntity>

    @Query("SELECT * FROM security_events WHERE status = 'IN_PROGRESS' AND timestamp < :before ORDER BY timestamp ASC")
    suspend fun getStaleInProgress(before: Long): List<SecurityEventEntity>

    @Query("SELECT * FROM security_events WHERE timestamp < :before AND status IN ('SENT','FAILED_FINAL','CANCELLED')")
    suspend fun getTerminalBefore(before: Long): List<SecurityEventEntity>

    @Query("DELETE FROM security_events WHERE timestamp < :before AND status IN ('SENT','FAILED_FINAL','CANCELLED')")
    suspend fun deleteOlderThan(before: Long)

    @Query("UPDATE security_events SET photoPath = NULL, audioPath = NULL WHERE timestamp < :before AND status IN ('SENT','FAILED_FINAL','CANCELLED')")
    suspend fun clearEvidencePathsBefore(before: Long)

    @Query("UPDATE security_events SET photoPath = NULL, audioPath = NULL WHERE eventId = :eventId")
    suspend fun clearEvidencePaths(eventId: String)

    @Query("DELETE FROM security_events WHERE eventId = :eventId")
    suspend fun deleteById(eventId: String)
}
