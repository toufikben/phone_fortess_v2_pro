package com.phonefortress.app.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomMigrationTest {
    @Test
    fun migration4To5PreservesEventsAndAddsRetryMetadata() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = File(context.cacheDir, "room-migration-test.db")
        dbFile.delete()
        val factory = FrameworkSQLiteOpenHelperFactory()
        val v4 = factory.create(config(context, dbFile, 4, ::createV4Schema))
        v4.writableDatabase.execSQL("INSERT INTO security_events(eventId,timestamp,failedAttempts,threshold,photoPath,audioPath,latitude,longitude,locationAccuracy,threatScore,threatLevel,threatReasons,status,isTest) VALUES ('legacy',1,3,3,NULL,NULL,NULL,NULL,NULL,20,'LOW','', 'PENDING',0)")
        v4.close()

        val v5 = factory.create(config(context, dbFile, 5) { db, oldVersion, newVersion ->
            assertThat(oldVersion).isEqualTo(4)
            assertThat(newVersion).isEqualTo(5)
            RoomMigrations.MIGRATION_4_5.migrate(db)
        })
        val database = v5.writableDatabase
        assertThat(database.version).isEqualTo(5)
        database.query("SELECT eventId, operation, lastTransitionReason FROM security_events WHERE eventId = 'legacy'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("eventId"))).isEqualTo("legacy")
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("operation"))).isEqualTo("CAPTURE")
            assertThat(cursor.isNull(cursor.getColumnIndexOrThrow("lastTransitionReason"))).isTrue()
        }
        v5.close()
        dbFile.delete()
    }

    @Test
    fun migration5To6AddsRecoveryMetadata() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = File(context.cacheDir, "room-migration-5-6-test.db")
        dbFile.delete()
        val factory = FrameworkSQLiteOpenHelperFactory()
        val v5 = factory.create(config(context, dbFile, 5, ::createV5Schema))
        v5.writableDatabase.execSQL(
            "INSERT INTO security_events(eventId,timestamp,failedAttempts,threshold,photoPath,audioPath,latitude,longitude,locationAccuracy,threatScore,threatLevel,threatReasons,status,operation,lastTransitionReason,isTest) VALUES ('legacy',100,3,3,NULL,NULL,NULL,NULL,NULL,20,'LOW','', 'IN_PROGRESS','CAPTURE',NULL,0)"
        )
        v5.close()

        val v6 = factory.create(config(context, dbFile, 6) { db, oldVersion, newVersion ->
            assertThat(oldVersion).isEqualTo(5)
            assertThat(newVersion).isEqualTo(6)
            RoomMigrations.MIGRATION_5_6.migrate(db)
        })
        val database = v6.writableDatabase
        database.query("SELECT retryCount, lastTransitionAt, sendClaimedAt FROM security_events WHERE eventId = 'legacy'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getInt(cursor.getColumnIndexOrThrow("retryCount"))).isEqualTo(0)
            assertThat(cursor.getLong(cursor.getColumnIndexOrThrow("lastTransitionAt"))).isEqualTo(100L)
            assertThat(cursor.isNull(cursor.getColumnIndexOrThrow("sendClaimedAt"))).isTrue()
        }
        v6.close()
        dbFile.delete()
    }

    @Test
    fun migration6To7AddsCaptureAndSendOwnershipTokens() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = File(context.cacheDir, "room-migration-6-7-test.db")
        dbFile.delete()
        val factory = FrameworkSQLiteOpenHelperFactory()
        val v6 = factory.create(config(context, dbFile, 6, ::createV6Schema))
        v6.writableDatabase.execSQL(
            "INSERT INTO security_events(eventId,timestamp,failedAttempts,threshold,photoPath,audioPath,latitude,longitude,locationAccuracy,threatScore,threatLevel,threatReasons,status,operation,lastTransitionReason,retryCount,lastTransitionAt,sendClaimedAt,isTest) VALUES ('legacy',200,1,3,NULL,NULL,NULL,NULL,NULL,10,'LOW','', 'SEND_PENDING','SEND',NULL,0,200,NULL,0)"
        )
        v6.close()

        val v7 = factory.create(config(context, dbFile, 7) { db, oldVersion, newVersion ->
            assertThat(oldVersion).isEqualTo(6)
            assertThat(newVersion).isEqualTo(7)
            RoomMigrations.MIGRATION_6_7.migrate(db)
        })
        val database = v7.writableDatabase
        assertThat(database.version).isEqualTo(7)
        database.query("SELECT timestamp, status, operation, retryCount, lastTransitionAt, sendClaimedAt, captureAttemptId, sendOwnerToken FROM security_events WHERE eventId = 'legacy'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))).isEqualTo(200L)
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("status"))).isEqualTo("SEND_PENDING")
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("operation"))).isEqualTo("SEND")
            assertThat(cursor.getInt(cursor.getColumnIndexOrThrow("retryCount"))).isEqualTo(0)
            assertThat(cursor.getLong(cursor.getColumnIndexOrThrow("lastTransitionAt"))).isEqualTo(200L)
            assertThat(cursor.isNull(cursor.getColumnIndexOrThrow("sendClaimedAt"))).isTrue()
            assertThat(cursor.isNull(cursor.getColumnIndexOrThrow("captureAttemptId"))).isTrue()
            assertThat(cursor.isNull(cursor.getColumnIndexOrThrow("sendOwnerToken"))).isTrue()
        }
        database.query("PRAGMA table_info(security_events)").use { cursor ->
            val columns = mutableMapOf<String, Pair<String, Int>>()
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val typeIndex = cursor.getColumnIndexOrThrow("type")
            val notNullIndex = cursor.getColumnIndexOrThrow("notnull")
            while (cursor.moveToNext()) {
                columns[cursor.getString(nameIndex)] = cursor.getString(typeIndex) to cursor.getInt(notNullIndex)
            }
            assertThat(columns["captureAttemptId"]).isEqualTo("TEXT" to 0)
            assertThat(columns["sendOwnerToken"]).isEqualTo("TEXT" to 0)
        }
        database.execSQL("UPDATE security_events SET captureAttemptId = 'attempt-1', sendOwnerToken = 'owner-1' WHERE eventId = 'legacy'")
        database.query("SELECT captureAttemptId, sendOwnerToken FROM security_events WHERE eventId = 'legacy'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("captureAttemptId"))).isEqualTo("attempt-1")
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("sendOwnerToken"))).isEqualTo("owner-1")
        }
        v7.close()
        dbFile.delete()
    }

    @Test
    fun migration6To7HandlesEmptyEventsTable() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = File(context.cacheDir, "room-migration-6-7-empty-test.db")
        dbFile.delete()
        val factory = FrameworkSQLiteOpenHelperFactory()
        val v6 = factory.create(config(context, dbFile, 6, ::createV6Schema))
        v6.writableDatabase
        v6.close()

        val v7 = factory.create(config(context, dbFile, 7) { db, oldVersion, newVersion ->
            assertThat(oldVersion).isEqualTo(6)
            assertThat(newVersion).isEqualTo(7)
            RoomMigrations.MIGRATION_6_7.migrate(db)
        })
        val database = v7.writableDatabase
        database.query("SELECT captureAttemptId, sendOwnerToken FROM security_events").use { cursor ->
            assertThat(cursor.moveToFirst()).isFalse()
        }
        v7.close()
        dbFile.delete()
    }

    @Test
    fun chainedMigration4To7PreservesLegacyEventAndBuildsFinalSchema() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = File(context.cacheDir, "room-migration-4-7-chained-test.db")
        dbFile.delete()
        val factory = FrameworkSQLiteOpenHelperFactory()
        val v4 = factory.create(config(context, dbFile, 4, ::createV4Schema))
        v4.writableDatabase.execSQL(
            "INSERT INTO security_events(eventId,timestamp,failedAttempts,threshold,photoPath,audioPath,latitude,longitude,locationAccuracy,threatScore,threatLevel,threatReasons,status,isTest) VALUES ('chained',400,4,5,'/legacy/photo.jpg','/legacy/audio.3gp',51.5,-0.1,7.5,80,'HIGH','legacy-reason', 'PENDING',1)"
        )
        v4.close()

        val v7 = factory.create(config(context, dbFile, 7) { db, oldVersion, newVersion ->
            assertThat(oldVersion).isEqualTo(4)
            assertThat(newVersion).isEqualTo(7)
            RoomMigrations.MIGRATION_4_5.migrate(db)
            RoomMigrations.MIGRATION_5_6.migrate(db)
            RoomMigrations.MIGRATION_6_7.migrate(db)
        })
        val database = v7.writableDatabase
        assertThat(database.version).isEqualTo(7)
        database.query(
            "SELECT eventId,timestamp,failedAttempts,threshold,photoPath,audioPath,latitude,longitude,locationAccuracy,threatScore,threatLevel,threatReasons,status,isTest,operation,lastTransitionReason,retryCount,lastTransitionAt,sendClaimedAt,captureAttemptId,sendOwnerToken FROM security_events WHERE eventId = 'chained'"
        ).use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("eventId"))).isEqualTo("chained")
            assertThat(cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))).isEqualTo(400L)
            assertThat(cursor.getInt(cursor.getColumnIndexOrThrow("failedAttempts"))).isEqualTo(4)
            assertThat(cursor.getInt(cursor.getColumnIndexOrThrow("threshold"))).isEqualTo(5)
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("photoPath"))).isEqualTo("/legacy/photo.jpg")
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("audioPath"))).isEqualTo("/legacy/audio.3gp")
            assertThat(cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"))).isEqualTo(51.5)
            assertThat(cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))).isEqualTo(-0.1)
            assertThat(cursor.getFloat(cursor.getColumnIndexOrThrow("locationAccuracy"))).isEqualTo(7.5f)
            assertThat(cursor.getInt(cursor.getColumnIndexOrThrow("threatScore"))).isEqualTo(80)
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("threatLevel"))).isEqualTo("HIGH")
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("threatReasons"))).isEqualTo("legacy-reason")
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("status"))).isEqualTo("PENDING")
            assertThat(cursor.getInt(cursor.getColumnIndexOrThrow("isTest"))).isEqualTo(1)
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("operation"))).isEqualTo("CAPTURE")
            assertThat(cursor.isNull(cursor.getColumnIndexOrThrow("lastTransitionReason"))).isTrue()
            assertThat(cursor.getInt(cursor.getColumnIndexOrThrow("retryCount"))).isEqualTo(0)
            assertThat(cursor.getLong(cursor.getColumnIndexOrThrow("lastTransitionAt"))).isEqualTo(400L)
            assertThat(cursor.isNull(cursor.getColumnIndexOrThrow("sendClaimedAt"))).isTrue()
            assertThat(cursor.isNull(cursor.getColumnIndexOrThrow("captureAttemptId"))).isTrue()
            assertThat(cursor.isNull(cursor.getColumnIndexOrThrow("sendOwnerToken"))).isTrue()
        }
        v7.close()
        dbFile.delete()
    }

    private fun config(
        context: Context,
        dbFile: File,
        version: Int,
        onCreate: ((SupportSQLiteDatabase) -> Unit)? = null,
        onUpgrade: (SupportSQLiteDatabase, Int, Int) -> Unit = { _, _, _ -> }
    ): SupportSQLiteOpenHelper.Configuration =
        SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbFile.name)
            .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) { onCreate?.invoke(db) }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = onUpgrade(db, oldVersion, newVersion)
            })
            .build()

    private fun createV4Schema(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS alert_logs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, eventId TEXT NOT NULL, channelId TEXT NOT NULL, status TEXT NOT NULL, messageId TEXT, reason TEXT, timestamp INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_alert_logs_eventId ON alert_logs(eventId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_alert_logs_channelId ON alert_logs(channelId)")
        db.execSQL("CREATE TABLE IF NOT EXISTS security_events (eventId TEXT NOT NULL, timestamp INTEGER NOT NULL, failedAttempts INTEGER NOT NULL, threshold INTEGER NOT NULL, photoPath TEXT, audioPath TEXT, latitude REAL, longitude REAL, locationAccuracy REAL, threatScore INTEGER NOT NULL, threatLevel TEXT NOT NULL, threatReasons TEXT NOT NULL, status TEXT NOT NULL, isTest INTEGER NOT NULL, PRIMARY KEY(eventId))")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_security_events_timestamp ON security_events(timestamp)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_security_events_status ON security_events(status)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_security_events_eventId ON security_events(eventId)")
        db.execSQL("CREATE TABLE IF NOT EXISTS safe_zones (id TEXT NOT NULL, name TEXT NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, radiusMeters REAL NOT NULL, type TEXT NOT NULL, enabled INTEGER NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
    }

    private fun createV5Schema(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS security_events (eventId TEXT NOT NULL, timestamp INTEGER NOT NULL, failedAttempts INTEGER NOT NULL, threshold INTEGER NOT NULL, photoPath TEXT, audioPath TEXT, latitude REAL, longitude REAL, locationAccuracy REAL, threatScore INTEGER NOT NULL, threatLevel TEXT NOT NULL, threatReasons TEXT NOT NULL, status TEXT NOT NULL, operation TEXT NOT NULL, lastTransitionReason TEXT, isTest INTEGER NOT NULL, PRIMARY KEY(eventId))")
    }

    private fun createV6Schema(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS security_events (eventId TEXT NOT NULL, timestamp INTEGER NOT NULL, failedAttempts INTEGER NOT NULL, threshold INTEGER NOT NULL, photoPath TEXT, audioPath TEXT, latitude REAL, longitude REAL, locationAccuracy REAL, threatScore INTEGER NOT NULL, threatLevel TEXT NOT NULL, threatReasons TEXT NOT NULL, status TEXT NOT NULL, operation TEXT NOT NULL, lastTransitionReason TEXT, retryCount INTEGER NOT NULL, lastTransitionAt INTEGER NOT NULL, sendClaimedAt INTEGER, isTest INTEGER NOT NULL, PRIMARY KEY(eventId))")
    }
}
