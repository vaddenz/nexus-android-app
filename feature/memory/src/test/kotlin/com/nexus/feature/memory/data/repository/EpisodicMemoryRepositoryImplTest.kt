package com.nexus.feature.memory.data.repository

import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.nexus.feature.memory.data.local.EpisodicMemoryDatabase
import com.nexus.feature.memory.domain.model.EpisodicEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class EpisodicMemoryRepositoryImplTest {

    private lateinit var db: EpisodicMemoryDatabase
    private lateinit var repository: EpisodicMemoryRepositoryImpl

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            EpisodicMemoryDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = EpisodicMemoryRepositoryImpl(db.eventDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `record and observeRecent returns event`() = runTest {
        val event = sampleEvent(id = "1", content = "hello")
        repository.record(event)

        val recent = repository.observeRecent(limit = 10).first()
        assertThat(recent).hasSize(1)
        assertThat(recent[0].contentText).isEqualTo("hello")
    }

    @Test
    fun `observeRecent respects limit`() = runTest {
        repeat(5) { i ->
            repository.record(sampleEvent(id = "$i", content = "msg-$i"))
        }

        val recent = repository.observeRecent(limit = 3).first()
        assertThat(recent).hasSize(3)
    }

    @Test
    fun `cleanup removes old events`() = runTest {
        val oldEvent = sampleEvent(id = "old", collectedAt = 1000L)
        val newEvent = sampleEvent(id = "new", collectedAt = 5000L)
        repository.record(oldEvent)
        repository.record(newEvent)

        val deleted = repository.cleanup(cutoffMs = 3000L)
        assertThat(deleted).isEqualTo(1)

        val recent = repository.observeRecent(limit = 10).first()
        assertThat(recent).hasSize(1)
        assertThat(recent[0].id).isEqualTo("new")
    }

    @Test
    fun `queryByPackage filters correctly`() = runTest {
        repository.record(sampleEvent(id = "1", packageName = "com.tencent.mm"))
        repository.record(sampleEvent(id = "2", packageName = "com.Slack"))

        val result = repository.queryByPackage("com.Slack", limit = 10)
        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo("2")
    }

    private fun sampleEvent(
        id: String,
        content: String = "content",
        packageName: String = "com.tencent.mm",
        collectedAt: Long = System.currentTimeMillis(),
    ) = EpisodicEvent(
        id = id,
        eventType = "im_message",
        sourcePackage = packageName,
        senderName = "sender",
        contentText = content,
        occurredAt = collectedAt,
        collectedAt = collectedAt,
        confidence = 0.9f,
        isDesensitized = false,
    )
}
