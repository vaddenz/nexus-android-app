package com.nexus.harness.sensor

import com.google.common.truth.Truth.assertThat
import com.nexus.core.common.coroutines.DispatcherProvider
import com.nexus.core.common.time.TimeProvider
import com.nexus.core.eventbus.FlowEventBus
import com.nexus.core.eventbus.subscribe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SensorSchedulerTest {

    private fun TestScope.scheduler(): Triple<SensorScheduler, SensorRegistry, FlowEventBus> {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dispatchers = TestDispatchers(dispatcher)
        val bus = FlowEventBus(extraBufferCapacity = 32)
        val registry = SensorRegistry()
        val scheduler = SensorScheduler(
            parentScope = CoroutineScope(coroutineContext),
            registry = registry,
            eventBus = bus,
            dispatchers = dispatchers,
            time = TimeProvider { 0L },
        )
        return Triple(scheduler, registry, bus)
    }

    @Test
    fun `polling sensor emits at fixed interval`() = runTest {
        val (scheduler, registry, bus) = scheduler()
        val counter = object : Sensor<Int> {
            override val id = "counter"
            override val kind = SensorKind.SYSTEM
            override val policy = SensorPolicy.Polling(100L)
            private var count = 0
            override suspend fun sample(): Int = ++count
        }
        registry.register(counter)
        val collected = mutableListOf<SensorObservation>()
        val collectJob = launch {
            bus.subscribe<SensorObservation>().take(3).toList(collected)
        }
        advanceUntilIdle()
        scheduler.activate("counter")
        advanceTimeBy(250)
        advanceUntilIdle()
        collectJob.join()

        assertThat(collected.map { it.value as Int }).containsExactly(1, 2, 3).inOrder()
        scheduler.deactivate("counter")
    }

    @Test
    fun `reactive sensor forwards observe stream`() = runTest {
        val (scheduler, registry, bus) = scheduler()
        val source = object : Sensor<String> {
            override val id = "reactive"
            override val kind = SensorKind.USER
            override val policy = SensorPolicy.Reactive
            override fun observe(): Flow<String> = flow {
                emit("a"); emit("b"); emit("c")
            }
        }
        registry.register(source)

        val collected = mutableListOf<SensorObservation>()
        val collectJob = launch {
            bus.subscribe<SensorObservation>().take(3).toList(collected)
        }
        advanceUntilIdle()
        scheduler.activate("reactive")
        advanceUntilIdle()
        collectJob.join()

        assertThat(collected.map { it.value as String }).containsExactly("a", "b", "c").inOrder()
    }

    @Test
    fun `activate is idempotent`() = runTest {
        val (scheduler, registry, _) = scheduler()
        registry.register(object : Sensor<Int> {
            override val id = "x"
            override val kind = SensorKind.SYSTEM
            override val policy = SensorPolicy.Polling(1000L)
            override suspend fun sample(): Int = 0
        })
        assertThat(scheduler.activate("x")).isTrue()
        assertThat(scheduler.activate("x")).isFalse()
    }

    @Test
    fun `activate unknown sensor returns false`() = runTest {
        val (scheduler, _, _) = scheduler()
        assertThat(scheduler.activate("missing")).isFalse()
    }

    @Test
    fun `sensor throwing in sample emits error event but keeps polling`() = runTest {
        val (scheduler, registry, bus) = scheduler()
        val flaky = object : Sensor<Int> {
            override val id = "flaky"
            override val kind = SensorKind.SYSTEM
            override val policy = SensorPolicy.Polling(50L)
            private var n = 0
            override suspend fun sample(): Int {
                n++
                if (n == 1) throw IllegalStateException("first call fails")
                return n
            }
        }
        registry.register(flaky)
        val errors = mutableListOf<SensorErrorEvent>()
        val obs = mutableListOf<SensorObservation>()
        val errJob = launch { bus.subscribe<SensorErrorEvent>().take(1).toList(errors) }
        val obsJob = launch { bus.subscribe<SensorObservation>().take(1).toList(obs) }
        advanceUntilIdle()
        scheduler.activate("flaky")
        advanceTimeBy(150)
        advanceUntilIdle()

        errJob.join(); obsJob.join()
        assertThat(errors).hasSize(1)
        assertThat(errors[0].cause).hasMessageThat().contains("first call fails")
        assertThat(obs).hasSize(1)
        assertThat(obs[0].value).isEqualTo(2)
        scheduler.deactivate("flaky")
    }

    @Test
    fun `deactivate cancels active sensor`() = runTest {
        val (scheduler, registry, _) = scheduler()
        var deactivated = false
        registry.register(object : Sensor<Int> {
            override val id = "stop-me"
            override val kind = SensorKind.SYSTEM
            override val policy = SensorPolicy.Polling(50L)
            override suspend fun sample(): Int = 0
            override suspend fun onDeactivate() { deactivated = true }
        })
        scheduler.activate("stop-me")
        advanceTimeBy(50)
        scheduler.deactivate("stop-me")
        advanceUntilIdle()
        assertThat(deactivated).isTrue()
        assertThat(scheduler.stateOf("stop-me")).isEqualTo(SensorState.IDLE)
    }

    @Test
    fun `lifecycle event emitted on activation and stop`() = runTest {
        val (scheduler, registry, bus) = scheduler()
        registry.register(object : Sensor<Int> {
            override val id = "lc"
            override val kind = SensorKind.SYSTEM
            override val policy = SensorPolicy.Polling(1000L)
            override suspend fun sample(): Int = 0
        })
        val events = mutableListOf<SensorLifecycleEvent>()
        val job = launch { bus.subscribe<SensorLifecycleEvent>().take(2).toList(events) }
        advanceUntilIdle()
        scheduler.activate("lc")
        advanceUntilIdle()
        scheduler.deactivate("lc")
        advanceUntilIdle()
        job.join()
        assertThat(events.map { it.state })
            .containsExactly(SensorState.ACTIVE, SensorState.STOPPED).inOrder()
    }
}

private class TestDispatchers(private val dispatcher: CoroutineDispatcher) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val unconfined: CoroutineDispatcher = dispatcher
}
