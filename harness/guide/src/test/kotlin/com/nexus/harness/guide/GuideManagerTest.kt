package com.nexus.harness.guide

import com.google.common.truth.Truth.assertThat
import com.nexus.core.common.coroutines.DispatcherProvider
import com.nexus.core.common.time.TimeProvider
import com.nexus.core.eventbus.FlowEventBus
import com.nexus.core.eventbus.subscribe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GuideManagerTest {

    private fun TestScope.newManager(time: () -> Long = { 0L }): Triple<GuideManager, FlowEventBus, RecordingGuide> {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dispatchers = TestDispatchers(dispatcher)
        val bus = FlowEventBus(extraBufferCapacity = 16)
        val manager = GuideManager(
            parentScope = CoroutineScope(coroutineContext),
            eventBus = bus,
            dispatchers = dispatchers,
            time = TimeProvider(time),
        )
        val guide = RecordingGuide()
        return Triple(manager, bus, guide)
    }

    @Test
    fun `register transitions to INITIALIZED and emits event`() = runTest {
        val (manager, bus, guide) = newManager()
        val received = mutableListOf<GuideStateChangedEvent>()
        val sub = launch { bus.subscribe<GuideStateChangedEvent>().take(1).toList(received) }
        advanceUntilIdle()
        manager.register(guide)
        advanceUntilIdle()
        sub.join()

        assertThat(manager.stateOf(guide.id)).isEqualTo(GuideState.INITIALIZED)
        assertThat(guide.calls).containsExactly("onInitialize")
        assertThat(received).hasSize(1)
        assertThat(received[0].to).isEqualTo(GuideState.INITIALIZED)
    }

    @Test
    fun `start then run drives guide through full happy path`() = runTest {
        val (manager, _, guide) = newManager()
        manager.register(guide)
        manager.start(guide.id)
        manager.run(guide.id)
        manager.pause(guide.id)
        manager.resume(guide.id)
        manager.stop(guide.id)

        assertThat(guide.calls)
            .containsExactly("onInitialize", "onStart", "onRun", "onPause", "onResume", "onStop")
            .inOrder()
        assertThat(manager.stateOf(guide.id)).isEqualTo(GuideState.STOPPED)
    }

    @Test
    fun `illegal transition throws`() = runTest {
        val (manager, _, guide) = newManager()
        manager.register(guide)
        // INITIALIZED -> RUNNING is illegal
        try {
            manager.run(guide.id)
            error("expected illegal state")
        } catch (e: IllegalStateException) {
            assertThat(e).hasMessageThat().contains("Illegal transition")
        }
    }

    @Test
    fun `callback throwing transitions to STOPPED and emits error event`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val bus = FlowEventBus(extraBufferCapacity = 16)
        val manager = GuideManager(
            parentScope = CoroutineScope(coroutineContext),
            eventBus = bus,
            dispatchers = TestDispatchers(dispatcher),
            time = TimeProvider { 1000L },
        )
        val boom = object : RecordingGuide(id = "boom") {
            override suspend fun onStart(context: GuideContext) {
                super.onStart(context)
                throw IllegalStateException("kaboom")
            }
        }
        manager.register(boom)
        val errorJob = launch { val e = bus.subscribe<GuideErrorEvent>().first(); errors += e }
        manager.start(boom.id)
        advanceUntilIdle()

        assertThat(manager.stateOf(boom.id)).isEqualTo(GuideState.STOPPED)
        errorJob.join()
        assertThat(errors).hasSize(1)
        assertThat(errors[0].cause).hasMessageThat().contains("kaboom")
    }

    @Test
    fun `destroy cancels guide scope and removes entry`() = runTest {
        val (manager, _, guide) = newManager()
        manager.register(guide)
        manager.destroy(guide.id)

        assertThat(manager.stateOf(guide.id)).isNull()
        assertThat(guide.calls).contains("onDestroy")
    }

    @Test
    fun `register is idempotent for same id`() = runTest {
        val (manager, _, guide) = newManager()
        manager.register(guide)
        manager.register(guide)
        // onInitialize should only have been called once
        assertThat(guide.calls.count { it == "onInitialize" }).isEqualTo(1)
    }

    @Test
    fun `destroyAll tears down every guide`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val bus = FlowEventBus(extraBufferCapacity = 16)
        val manager = GuideManager(
            parentScope = CoroutineScope(coroutineContext),
            eventBus = bus,
            dispatchers = TestDispatchers(dispatcher),
            time = TimeProvider { 0L },
        )
        val a = RecordingGuide(id = "a")
        val b = RecordingGuide(id = "b")
        manager.register(a)
        manager.register(b)
        manager.destroyAll()

        assertThat(manager.stateOf("a")).isNull()
        assertThat(manager.stateOf("b")).isNull()
        assertThat(a.calls).contains("onDestroy")
        assertThat(b.calls).contains("onDestroy")
    }

    private val errors = mutableListOf<GuideErrorEvent>()
}

internal open class RecordingGuide(
    override val id: String = "test-guide",
    override val name: String = "Test Guide",
) : Guide {
    val calls = mutableListOf<String>()
    override suspend fun onInitialize(context: GuideContext) { calls += "onInitialize" }
    override suspend fun onStart(context: GuideContext) { calls += "onStart" }
    override suspend fun onRun(context: GuideContext) { calls += "onRun" }
    override suspend fun onPause(context: GuideContext) { calls += "onPause" }
    override suspend fun onResume(context: GuideContext) { calls += "onResume" }
    override suspend fun onStop(context: GuideContext) { calls += "onStop" }
    override suspend fun onDestroy(context: GuideContext) { calls += "onDestroy" }
}

internal class TestDispatchers(private val dispatcher: CoroutineDispatcher) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val unconfined: CoroutineDispatcher = dispatcher
}
