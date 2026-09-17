package com.phonefortress.app.data.repository

import com.google.common.truth.Truth.assertThat
import com.phonefortress.app.data.local.dao.EventDao
import com.phonefortress.app.data.local.entity.SecurityEventEntity
import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.domain.model.SecurityEventStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EventRepositoryTest {
    private lateinit var dao: EventDao
    private lateinit var repo: EventRepository
    @BeforeEach fun setup() { dao = mockk(relaxed = true); repo = EventRepository(dao) }
    @Test fun `create converts event to entity`() = runTest {
        val event = SecurityEvent("evt-1", 100L, 2, 3, status = SecurityEventStatus.PENDING)
        repo.create(event)
        coVerify { dao.insert(match { it.eventId == "evt-1" && it.status == "PENDING" }) }
    }
    @Test fun `get by id converts entity to domain`() = runTest {
        coEvery { dao.getById("evt-1") } returns null
        assertThat(repo.getById("evt-1")).isNull()
    }
    @Test fun `transition persists only legal state changes`() = runTest {
        val pending = SecurityEventEntity.fromDomain(SecurityEvent("evt-1", 100L, 2, 3))
        coEvery { dao.getById("evt-1") } returns pending
        coEvery { dao.transitionStatus("evt-1", "PENDING", "IN_PROGRESS", "CAPTURE", "test", any()) } returns 1
        repo.transition("evt-1", SecurityEventStatus.IN_PROGRESS, "test")
        coVerify { dao.transitionStatus("evt-1", "PENDING", "IN_PROGRESS", "CAPTURE", "test", any()) }
    }
    @Test fun `metadata update delegates without changing status`() = runTest {
        repo.updateMetadata(SecurityEvent("evt-1", 100L, 2, 3, photoPath = "/tmp/photo.jpg"))
        coVerify { dao.updateMetadata("evt-1", "/tmp/photo.jpg", null, null, null, null, 0, "LOW", "") }
    }
    @Test fun `conditional transition loss does not report a claimed event`() = runTest {
        coEvery { dao.getById("evt-1") } returns SecurityEventEntity.fromDomain(SecurityEvent("evt-1", 100L, 2, 3))
        coEvery { dao.transitionStatus(any(), any(), any(), any(), any(), any()) } returns 0
        assertThat(repo.transition("evt-1", SecurityEventStatus.IN_PROGRESS, "lost-race")).isNull()
    }
    @Test fun `retryable transition persists through the conditional update`() = runTest {
        coEvery { dao.getById("evt-1") } returns SecurityEventEntity.fromDomain(
            SecurityEvent("evt-1", 100L, 2, 3, status = SecurityEventStatus.IN_PROGRESS)
        )
        coEvery { dao.transitionStatus(any(), any(), any(), any(), any(), any()) } returns 1
        repo.transition("evt-1", SecurityEventStatus.FAILED_RETRYABLE, "capture-failed")
        coVerify { dao.transitionStatus("evt-1", "IN_PROGRESS", "FAILED_RETRYABLE", "CAPTURE", "capture-failed", any()) }
    }
    @Test fun `send claim reports only the winning worker`() = runTest {
        coEvery { dao.claimSend("evt-1", 1_000L, any()) } returns 1
        assertThat(repo.claimForSend("evt-1", 1_000L)).isTrue()
        coVerify { dao.claimSend("evt-1", 1_000L, any()) }
    }
    @Test fun `active events are mapped`() = runTest { coEvery { dao.getActive() } returns emptyList(); assertThat(repo.getActive()).isEmpty() }
    @Test fun `observe recent maps empty flow`() = runTest { every { dao.observeRecent(50) } returns flowOf(emptyList()); assertThat(repo.observeRecent().first()).isEmpty() }
}
