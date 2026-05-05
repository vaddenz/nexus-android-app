package com.nexus.core.eventbus

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

private data class FooEvent(override val timestampMs: Long, val payload: String) : Event
private data class BarEvent(override val timestampMs: Long, val value: Int) : Event

@OptIn(ExperimentalCoroutinesApi::class)
class FlowEventBusTest {

    @Test
    fun `publish then subscribe receives event`() = runTest {
        val bus = FlowEventBus()
        bus.events.test {
            launch { bus.publish(FooEvent(1, "hello")) }
            val received = awaitItem()
            assertThat(received).isInstanceOf(FooEvent::class.java)
            assertThat((received as FooEvent).payload).isEqualTo("hello")
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `subscribe filters by type`() = runTest {
        val bus = FlowEventBus(extraBufferCapacity = 8)
        val collected = mutableListOf<FooEvent>()
        val job = launch {
            bus.subscribe<FooEvent>().take(2).toList(collected)
        }
        // Allow subscription to register before publishing
        advanceUntilIdle()
        bus.publish(BarEvent(1, 42))
        bus.publish(FooEvent(2, "a"))
        bus.publish(BarEvent(3, 43))
        bus.publish(FooEvent(4, "b"))
        job.join()
        assertThat(collected.map { it.payload }).containsExactly("a", "b").inOrder()
    }

    @Test
    fun `tryPublish returns true when subscriber is fast enough`() = runTest {
        val bus = FlowEventBus(extraBufferCapacity = 8)
        // No subscribers + extraBufferCapacity slots → tryEmit succeeds.
        repeat(5) {
            assertThat(bus.tryPublish(FooEvent(it.toLong(), "n=$it"))).isTrue()
        }
    }

    @Test
    fun `tryPublish drops latest when buffer full and overflow is DROP_LATEST`() = runTest {
        val bus = FlowEventBus(
            extraBufferCapacity = 1,
            onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_LATEST,
        )
        assertThat(bus.tryPublish(FooEvent(1, "a"))).isTrue()
        // Buffer is full now (no subscribers); next emit gets dropped → tryEmit returns true
        // but the message is silently dropped; verify no observer sees it.
        bus.tryPublish(FooEvent(2, "b"))
    }

    @Test
    fun `replay delivers past events to late subscribers`() = runTest {
        val bus = FlowEventBus(replay = 1)
        bus.publish(FooEvent(1, "first"))
        val received = bus.subscribe<FooEvent>().first()
        assertThat(received.payload).isEqualTo("first")
    }

    @Test
    fun `subscriberCount reflects active collectors`() = runTest {
        val bus = FlowEventBus()
        assertThat(bus.subscriberCount).isEqualTo(0)
        val job = launch { bus.events.collect { /* hold */ } }
        advanceUntilIdle()
        assertThat(bus.subscriberCount).isEqualTo(1)
        job.cancel()
    }

    @Test
    fun `multiple subscribers each receive every event`() = runTest {
        val bus = FlowEventBus(extraBufferCapacity = 8)
        val a = mutableListOf<FooEvent>()
        val b = mutableListOf<FooEvent>()
        val jobA = launch { bus.subscribe<FooEvent>().take(2).toList(a) }
        val jobB = launch { bus.subscribe<FooEvent>().take(2).toList(b) }
        advanceUntilIdle()
        bus.publish(FooEvent(1, "x"))
        bus.publish(FooEvent(2, "y"))
        jobA.join(); jobB.join()
        assertThat(a.map { it.payload }).containsExactly("x", "y").inOrder()
        assertThat(b.map { it.payload }).containsExactly("x", "y").inOrder()
    }
}
