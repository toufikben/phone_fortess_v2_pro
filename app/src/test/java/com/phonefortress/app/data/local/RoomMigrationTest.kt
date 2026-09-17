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
}
