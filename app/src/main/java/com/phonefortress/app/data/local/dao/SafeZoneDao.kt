package com.phonefortress.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.phonefortress.app.data.local.entity.SafeZoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SafeZoneDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(zone: SafeZoneEntity)

    @Query("SELECT * FROM safe_zones ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SafeZoneEntity>>

    @Query("SELECT * FROM safe_zones WHERE enabled = 1")
    suspend fun getAllEnabled(): List<SafeZoneEntity>

    @Query("SELECT * FROM safe_zones WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SafeZoneEntity?

    @Query("DELETE FROM safe_zones WHERE id = :id")
    suspend fun delete(id: String)
}
