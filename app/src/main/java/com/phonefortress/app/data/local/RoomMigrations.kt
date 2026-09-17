package com.phonefortress.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object RoomMigrations {
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE security_events ADD COLUMN operation TEXT NOT NULL DEFAULT 'CAPTURE'"
            )
            database.execSQL(
                "ALTER TABLE security_events ADD COLUMN lastTransitionReason TEXT DEFAULT NULL"
            )
        }
    }
}
