package com.phonefortress.app.security

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import com.google.common.truth.Truth.assertThat
import com.phonefortress.app.alerts.AlertDispatcher
import com.phonefortress.app.data.crypto.PinHasher
import com.phonefortress.app.data.local.AppDatabase
import com.phonefortress.app.data.prefs.PinPrefs
import com.phonefortress.app.data.prefs.SecurityPrefs
import com.phonefortress.app.data.repository.AlertRepository
import com.phonefortress.app.data.repository.EventRepository
import com.phonefortress.app.domain.model.AlertChannel
import com.phonefortress.app.domain.model.AlertPayload
import com.phonefortress.app.domain.model.AlertResult
import com.phonefortress.app.domain.model.EventOperation
import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.domain.model.SecurityEventStatus
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Batch93SecurityConcurrencyTest {
    private lateinit var database: AppDatabase
    private lateinit var events: EventRepository
    private lateinit var alerts: AlertRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        events = EventRepository(database.eventDao())
        alerts = AlertRepository(database.alertLogDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun staleCaptureAttemptCannotOverwriteReplacementAttempt() = runBlocking {
        val eventId = "capture-race-event"
        events.create(event(eventId))

        val attemptA = events.claimCapture(eventId, now = 1_000L)
        assertThat(attemptA).isNotNull()
        assertThat(events.transitionCaptureOwned(eventId, requireNotNull(attemptA), SecurityEventStatus.FAILED_RETRYABLE, "timeout")).isTrue()

        val attemptB = events.claimCapture(eventId, now = 2_000L)
        assertThat(attemptB).isNotNull()
        assertThat(attemptB).isNotEqualTo(attemptA)

        assertThat(events.transitionCaptureOwned(eventId, requireNotNull(attemptA), SecurityEventStatus.CAPTURED, "late-capture")).isFalse()

        val current = requireNotNull(events.getById(eventId))
        assertThat(current.status).isEqualTo(SecurityEventStatus.IN_PROGRESS)
        assertThat(current.operation).isEqualTo(EventOperation.CAPTURE)
        assertThat(current.captureAttemptId).isEqualTo(attemptB)
        assertThat(current.retryCount).isEqualTo(1)
        Unit
    }

    @Test
    fun staleSendOwnerCannotFinalizeEventAfterLeaseReclaim() = runBlocking {
        val eventId = "send-fencing-event"
        events.create(event(eventId))
        events.transition(eventId, SecurityEventStatus.IN_PROGRESS, "capture-start", EventOperation.CAPTURE)
        events.transition(eventId, SecurityEventStatus.CAPTURED, "capture-complete", EventOperation.CAPTURE)
        events.transition(eventId, SecurityEventStatus.SEND_PENDING, "dispatch-ready", EventOperation.SEND)

        val ownerA = events.claimForSend(eventId, now = 1_000_000L)
        assertThat(ownerA).isNotNull()
        val ownerB = events.claimForSend(eventId, now = 1_000_000L + 5 * 60_000L + 1L)
        assertThat(ownerB).isNotNull()
        assertThat(ownerB).isNotEqualTo(ownerA)

        assertThat(events.transitionSendOwned(eventId, SecurityEventStatus.SEND_PENDING, SecurityEventStatus.SENT, requireNotNull(ownerA), "stale-success")).isFalse()
        val afterStale = requireNotNull(events.getById(eventId))
        assertThat(afterStale.status).isEqualTo(SecurityEventStatus.SEND_PENDING)
        assertThat(afterStale.sendOwnerToken).isEqualTo(ownerB)

        assertThat(events.transitionSendOwned(eventId, SecurityEventStatus.SEND_PENDING, SecurityEventStatus.SENT, requireNotNull(ownerB), "current-success")).isTrue()
        val completed = requireNotNull(events.getById(eventId))
        assertThat(completed.status).isEqualTo(SecurityEventStatus.SENT)
        assertThat(completed.retryCount).isEqualTo(0)
        Unit
    }

    @Test
    fun partialChannelRetrySkipsSuccessfulChannelAndCompletesAfterFailedChannelRecovers() = runBlocking {
        val channelA = CountingChannel("channel-a") { AlertResult.Success("message-a", "channel-a") }
        var channelBAttempts = 0
        val channelB = CountingChannel("channel-b") {
            channelBAttempts += 1
            if (channelBAttempts == 1) AlertResult.Retryable("temporary", "channel-b")
            else AlertResult.Success("message-b", "channel-b")
        }
        val dispatcher = AlertDispatcher(setOf(channelA, channelB), alerts)
        val event = event("partial-retry-event")

        val first = dispatcher.dispatch(event)
        assertThat(first).hasSize(2)
        assertThat(channelA.sendCount).isEqualTo(1)
        assertThat(channelB.sendCount).isEqualTo(1)
        assertThat(alerts.getSuccessfulChannels(event.id)).containsExactly("channel-a")

        val second = dispatcher.dispatch(event)
        assertThat(second).containsExactly(AlertResult.Success("message-b", "channel-b"))
        assertThat(channelA.sendCount).isEqualTo(1)
        assertThat(channelB.sendCount).isEqualTo(2)

        val logs = alerts.getByEvent(event.id)
        assertThat(logs.count { it.channelId == "channel-a" && it.status == "SUCCESS" }).isEqualTo(1)
        assertThat(logs.count { it.channelId == "channel-b" && it.status == "RETRYABLE" }).isEqualTo(1)
        assertThat(logs.count { it.channelId == "channel-b" && it.status == "SUCCESS" }).isEqualTo(1)
        assertThat(alerts.getSuccessfulChannels(event.id)).containsExactly("channel-a", "channel-b")
        Unit
    }

    @Test
    fun corruptedSecurityDataStoreFailsClosedAndPinCorruptionCannotAuthenticate() = runBlocking {
        val isolatedContext = context.createDeviceProtectedStorageContext()
        val securityFile = isolatedContext.preferencesDataStoreFile("batch93-security-corruption")
        corruptDataStoreFile(securityFile)

        val recoveredSecurity = SecurityPrefs(newPreferenceDataStore(securityFile))
        assertThat(recoveredSecurity.protectionEnabled.first()).isTrue()
        assertThat(recoveredSecurity.threshold.first()).isEqualTo(com.phonefortress.app.util.Constants.MIN_THRESHOLD)

        val pinFile = isolatedContext.preferencesDataStoreFile("batch93-pin-corruption")
        corruptDataStoreFile(pinFile)

        val recoveredPin = PinPrefs(newPreferenceDataStore(pinFile), PinHasher())
        assertThat(recoveredPin.isPinEnabled.first()).isTrue()
        assertThat(recoveredPin.verifyPin("2468")).isEqualTo(PinPrefs.VerifyResult.Corrupted)
        Unit
    }

    private fun corruptDataStoreFile(file: java.io.File) {
        file.parentFile?.mkdirs()
        file.delete()
        // Field tag zero is forbidden by protobuf and is rejected immediately by
        // PreferencesMapCompat, which deterministically reaches the corruption handler.
        file.writeBytes(byteArrayOf(0x00))
    }

    private fun newPreferenceDataStore(file: java.io.File): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler {
                preferencesOf(booleanPreferencesKey("corruption_detected") to true)
            },
            produceFile = { file }
        )

    private fun event(id: String) = SecurityEvent(
        id = id,
        timestamp = 100L,
        failedAttempts = 1,
        threshold = 3,
        status = SecurityEventStatus.PENDING
    )

    private class CountingChannel(
        override val id: String,
        private val result: suspend () -> AlertResult
    ) : AlertChannel {
        var sendCount: Int = 0
            private set
        override val displayName: String = id
        override val requiresConfig: Boolean = false
        override suspend fun isEnabled(): Boolean = true
        override suspend fun isConfigured(): Boolean = true
        override suspend fun send(event: SecurityEvent, payload: AlertPayload): AlertResult {
            sendCount += 1
            return result()
        }
        override suspend fun test(): AlertResult = result()
    }
}
