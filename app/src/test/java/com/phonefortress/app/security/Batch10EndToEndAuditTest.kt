package com.phonefortress.app.security

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.phonefortress.app.alerts.AlertDispatcher
import com.phonefortress.app.data.local.AppDatabase
import com.phonefortress.app.data.repository.AlertRepository
import com.phonefortress.app.data.repository.EventRepository
import com.phonefortress.app.domain.model.AlertChannel
import com.phonefortress.app.domain.model.AlertPayload
import com.phonefortress.app.domain.model.AlertResult
import com.phonefortress.app.domain.model.EventOperation
import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.domain.model.SecurityEventStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Batch10EndToEndAuditTest {
    private lateinit var database: AppDatabase
    private lateinit var events: EventRepository
    private lateinit var alerts: AlertRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
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
    fun durableChannelSuccessSurvivesProcessGapBeforeSentTransition() = runBlocking {
        val channel = CountingChannel("channel-a")
        val dispatcher = AlertDispatcher(setOf(channel), alerts)
        val eventId = "batch10-process-gap"
        events.create(event(eventId))
        events.transition(eventId, SecurityEventStatus.IN_PROGRESS, "capture-start", EventOperation.CAPTURE)
        events.transition(eventId, SecurityEventStatus.CAPTURED, "capture-complete", EventOperation.CAPTURE)
        events.transition(eventId, SecurityEventStatus.SEND_PENDING, "dispatch-ready", EventOperation.SEND)

        val first = requireNotNull(events.getById(eventId))
        val firstOwner = requireNotNull(events.claimForSend(eventId, now = 1_000L))
        assertThat(dispatcher.dispatch(first)).containsExactly(AlertResult.Success("message-1", "channel-a"))
        assertThat(channel.sendCount).isEqualTo(1)
        assertThat(requireNotNull(events.getById(eventId)).status).isEqualTo(SecurityEventStatus.SEND_PENDING)

        val recreatedDispatcher = AlertDispatcher(setOf(channel), alerts)
        val second = requireNotNull(events.getById(eventId))
        val secondOwner = requireNotNull(events.claimForSend(eventId, now = 5 * 60_000L + 1_001L))
        val recovered = recreatedDispatcher.dispatch(second)
        assertThat(recovered).containsExactly(AlertResult.Success("already-delivered", "channel-a"))
        assertThat(channel.sendCount).isEqualTo(1)
        assertThat(events.transitionSendOwned(eventId, SecurityEventStatus.SEND_PENDING, SecurityEventStatus.SENT, secondOwner, "dispatch-success-after-restart")).isTrue()
        assertThat(requireNotNull(events.getById(eventId)).status).isEqualTo(SecurityEventStatus.SENT)
        assertThat(firstOwner).isNotEqualTo(secondOwner)
    }

    private fun event(id: String) = SecurityEvent(
        id = id,
        timestamp = 100L,
        failedAttempts = 1,
        threshold = 3,
        status = SecurityEventStatus.PENDING
    )

    private class CountingChannel(
        override val id: String,
        var sendCount: Int = 0
    ) : AlertChannel {
        override val displayName: String = id
        override val requiresConfig: Boolean = false
        override suspend fun isEnabled(): Boolean = true
        override suspend fun isConfigured(): Boolean = true
        override suspend fun send(event: SecurityEvent, payload: AlertPayload): AlertResult {
            sendCount += 1
            return AlertResult.Success("message-1", id)
        }
        override suspend fun test(): AlertResult = AlertResult.Success("test", id)
    }
}
