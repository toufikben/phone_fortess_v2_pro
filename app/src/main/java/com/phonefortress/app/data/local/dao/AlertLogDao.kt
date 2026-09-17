package com.phonefortress.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.phonefortress.app.data.local.entity.AlertLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertLogDao {

    @Insert
    suspend fun insert(log: AlertLogEntity): Long

    @Query("SELECT * FROM alert_logs WHERE eventId = :eventId ORDER BY timestamp DESC")
    suspend fun getByEvent(eventId: String): List<AlertLogEntity>

    @Query("SELECT * FROM alert_logs ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<AlertLogEntity>>

    @Query("DELETE FROM alert_logs WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}
