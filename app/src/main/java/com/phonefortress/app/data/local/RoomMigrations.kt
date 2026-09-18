package com.phonefortress.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object RoomMigrations {
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE security_events ADD COLUMN captureAttemptId TEXT DEFAULT NULL")
            database.execSQL("ALTER TABLE security_events ADD COLUMN sendOwnerToken TEXT DEFAULT NULL")
        }
    }
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

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE security_events ADD COLUMN retryCount INTEGER NOT NULL DEFAULT 0"
            )
            database.execSQL(
                "ALTER TABLE security_events ADD COLUMN lastTransitionAt INTEGER NOT NULL DEFAULT 0"
            )
            database.execSQL(
                "ALTER TABLE security_events ADD COLUMN sendClaimedAt INTEGER DEFAULT NULL"
            )
            database.execSQL(
                "UPDATE security_events SET lastTransitionAt = timestamp WHERE lastTransitionAt = 0"
            )
        }
    }
}
