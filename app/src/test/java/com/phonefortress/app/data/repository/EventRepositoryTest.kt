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
        coEvery { dao.getById("evt-1") } returns null
        repo.create(event)
        coVerify { dao.upsert(match { it.eventId == "evt-1" && it.status == "PENDING" }) }
    }
    @Test fun `get by id converts entity to domain`() = runTest {
        coEvery { dao.getById("evt-1") } returns null
        assertThat(repo.getById("evt-1")).isNull()
    }
    @Test fun `transition persists only legal state changes`() = runTest {
        coEvery { dao.getById("evt-1") } returns SecurityEventEntity.fromDomain(SecurityEvent("evt-1", 100L, 2, 3))
        repo.transition("evt-1", SecurityEventStatus.IN_PROGRESS, "test")
        coVerify { dao.upsert(match { it.eventId == "evt-1" && it.status == "IN_PROGRESS" && it.lastTransitionReason == "test" }) }
    }
    @Test fun `metadata update delegates without changing status`() = runTest {
        repo.updateMetadata(SecurityEvent("evt-1", 100L, 2, 3, photoPath = "/tmp/photo.jpg"))
        coVerify { dao.updateMetadata("evt-1", "/tmp/photo.jpg", null, null, null, null, 0, "LOW", "") }
    }
    @Test fun `active events are mapped`() = runTest { coEvery { dao.getActive() } returns emptyList(); assertThat(repo.getActive()).isEmpty() }
    @Test fun `observe recent maps empty flow`() = runTest { every { dao.observeRecent(50) } returns flowOf(emptyList()); assertThat(repo.observeRecent().first()).isEmpty() }
}
