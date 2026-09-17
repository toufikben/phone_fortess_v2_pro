package com.phonefortress.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.phonefortress.app.data.local.dao.AlertLogDao
import com.phonefortress.app.data.local.dao.EventDao
import com.phonefortress.app.data.local.dao.SafeZoneDao
import com.phonefortress.app.data.local.entity.AlertLogEntity
import com.phonefortress.app.data.local.entity.SafeZoneEntity
import com.phonefortress.app.data.local.entity.SecurityEventEntity

@Database(
    entities = [
        AlertLogEntity::class,
        SecurityEventEntity::class,
        SafeZoneEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alertLogDao(): AlertLogDao
    abstract fun eventDao(): EventDao
    abstract fun safeZoneDao(): SafeZoneDao
}
