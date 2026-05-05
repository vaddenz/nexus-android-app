package com.nexus.harness.pipeline

import com.google.common.truth.Truth.assertThat
import com.nexus.core.eventbus.Event
import com.nexus.core.eventbus.EventBus
import com.nexus.core.eventbus.FlowEventBus
import com.nexus.core.eventbus.subscribe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

private data class IntEvent(val value: Int, override val timestampMs: Long) : Event
private data class SumContext(
    override val pipelineId: String,
    val total: Int,
    override val timestampMs: Long,
) : DataContext

@OptIn(ExperimentalCoroutinesApi::class)
class PipelineRunnerTest {

    @Test
    fun `pipeline output is republished on the bus`() = runTest {
        val bus = FlowEventBus(extraBufferCapacity = 16)
        val registry = PipelineRegistry()
        registry.register(object : Pipeline<SumContext> {
            override val id = "sum"
            override fun build(bus: EventBus): Flow<SumContext> =
                bus.subscribe<IntEvent>().map { SumContext("sum", it.value * 2, it.timestampMs) }
        })
        val runner = PipelineRunner(CoroutineScope(coroutineContext), registry, bus)
        val received = mutableListOf<SumContext>()
        val job = launch { bus.subscribe<SumContext>().take(2).toList(received) }
        advanceUntilIdle()
        runner.start("sum")
        advanceUntilIdle()
        bus.publish(IntEvent(2, 0))
        bus.publish(IntEvent(3, 0))
        advanceUntilIdle()
        job.join()
        runner.stop("sum")
        assertThat(received.map { it.total }).containsExactly(4, 6).inOrder()
    }

    @Test
    fun `start is idempotent`() = runTest {
        val bus = FlowEventBus()
        val registry = PipelineRegistry()
        registry.register(object : Pipeline<SumContext> {
            override val id = "p"
            override fun build(bus: EventBus): Flow<SumContext> = flow {}
        })
        val runner = PipelineRunner(CoroutineScope(coroutineContext), registry, bus)
        assertThat(runner.start("p")).isTrue()
        assertThat(runner.start("p")).isFalse()
        runner.stop("p")
    }

    @Test
    fun `start unknown returns false`() = runTest {
        val bus = FlowEventBus()
        val registry = PipelineRegistry()
        val runner = PipelineRunner(CoroutineScope(coroutineContext), registry, bus)
        assertThat(runner.start("missing")).isFalse()
    }

    @Test
    fun `pipeline error is forwarded as PipelineErrorEvent`() = runTest {
        val bus = FlowEventBus(extraBufferCapacity = 8)
        val registry = PipelineRegistry()
        registry.register(object : Pipeline<SumContext> {
            override val id = "boom"
            override fun build(bus: EventBus): Flow<SumContext> = flow {
                throw IllegalStateException("explode")
            }
        })
        val runner = PipelineRunner(CoroutineScope(coroutineContext), registry, bus)
        val errors = mutableListOf<PipelineErrorEvent>()
        val job = launch { bus.subscribe<PipelineErrorEvent>().take(1).toList(errors) }
        advanceUntilIdle()
        runner.start("boom")
        advanceUntilIdle()
        job.join()
        assertThat(errors).hasSize(1)
        assertThat(errors[0].cause).hasMessageThat().contains("explode")
        assertThat(errors[0].pipelineId).isEqualTo("boom")
    }

    @Test
    fun `stopAll cancels every pipeline`() = runTest {
        val bus = FlowEventBus()
        val registry = PipelineRegistry()
        registry.register(object : Pipeline<SumContext> {
            override val id = "p1"
            override fun build(bus: EventBus): Flow<SumContext> = flow {}
        })
        registry.register(object : Pipeline<SumContext> {
            override val id = "p2"
            override fun build(bus: EventBus): Flow<SumContext> = flow {}
        })
        val runner = PipelineRunner(CoroutineScope(coroutineContext), registry, bus)
        runner.startAll()
        assertThat(runner.isRunning("p1")).isTrue()
        assertThat(runner.isRunning("p2")).isTrue()
        runner.stopAll()
        assertThat(runner.isRunning("p1")).isFalse()
        assertThat(runner.isRunning("p2")).isFalse()
    }

    @Test
    fun `registry rejects duplicate id`() {
        val registry = PipelineRegistry()
        val p = object : Pipeline<SumContext> {
            override val id = "x"
            override fun build(bus: EventBus): Flow<SumContext> = flow {}
        }
        assertThat(registry.register(p)).isTrue()
        assertThat(registry.register(p)).isFalse()
    }
}
