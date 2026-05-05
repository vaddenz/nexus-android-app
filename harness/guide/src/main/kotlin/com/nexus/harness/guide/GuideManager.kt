package com.nexus.harness.guide

import com.nexus.core.common.coroutines.DispatcherProvider
import com.nexus.core.common.time.TimeProvider
import com.nexus.core.eventbus.EventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Orchestrates the lifecycle of registered [Guide] instances.
 *
 * - Each guide has its own supervisor scope, so a crash in one guide does not propagate.
 * - All transitions are serialized per guide via a per-guide [Mutex] — no interleaved callbacks.
 * - [destroy] cancels every guide's scope, guaranteeing no leaked coroutines.
 */
class GuideManager(
    private val parentScope: CoroutineScope,
    private val eventBus: EventBus,
    private val dispatchers: DispatcherProvider,
    private val time: TimeProvider,
) {

    private data class Entry(
        val guide: Guide,
        val scope: CoroutineScope,
        val mutex: Mutex,
        val job: Job,
        val state: MutableStateFlow<GuideState>,
    )

    private val entries = ConcurrentHashMap<String, Entry>()

    /** Reactive view of every registered guide's current state. */
    val states: Map<String, StateFlow<GuideState>>
        get() = entries.mapValues { it.value.state.asStateFlow() }

    /** Returns the current state of [guideId], or null if not registered. */
    fun stateOf(guideId: String): GuideState? = entries[guideId]?.state?.value

    /**
     * Register a guide and run its [Guide.onInitialize]. Idempotent: re-registering
     * the same id returns the existing state without re-initializing.
     */
    suspend fun register(guide: Guide) {
        if (entries.containsKey(guide.id)) return
        val job = SupervisorJob(parent = parentScope.coroutineContext[Job])
        val scope = CoroutineScope(parentScope.coroutineContext + job + dispatchers.default)
        val state = MutableStateFlow(GuideState.INITIALIZED)
        val entry = Entry(guide, scope, Mutex(), job, state)
        entries[guide.id] = entry
        runCallback(entry, GuideState.INITIALIZED) { ctx -> guide.onInitialize(ctx) }
    }

    suspend fun start(guideId: String) = transition(guideId, GuideState.STARTED) { ctx, guide ->
        guide.onStart(ctx)
    }

    suspend fun run(guideId: String) = transition(guideId, GuideState.RUNNING) { ctx, guide ->
        guide.onRun(ctx)
    }

    suspend fun pause(guideId: String) = transition(guideId, GuideState.PAUSED) { ctx, guide ->
        guide.onPause(ctx)
    }

    suspend fun resume(guideId: String) = transition(guideId, GuideState.RUNNING) { ctx, guide ->
        guide.onResume(ctx)
    }

    suspend fun stop(guideId: String) = transition(guideId, GuideState.STOPPED) { ctx, guide ->
        guide.onStop(ctx)
    }

    /** Tear down a guide. Cancels its scope; subsequent calls are no-ops. */
    suspend fun destroy(guideId: String) {
        val entry = entries[guideId] ?: return
        transition(guideId, GuideState.DESTROYED) { ctx, guide ->
            guide.onDestroy(ctx)
        }
        entry.scope.cancel()
        entries.remove(guideId)
    }

    /** Tear down every guide. Used during application shutdown. */
    suspend fun destroyAll() {
        entries.keys.toList().forEach { destroy(it) }
    }

    private suspend fun transition(
        guideId: String,
        target: GuideState,
        block: suspend (GuideContext, Guide) -> Unit,
    ) {
        val entry = entries[guideId] ?: error("Guide $guideId is not registered")
        entry.mutex.withLock {
            val current = entry.state.value
            if (!current.canTransitionTo(target)) {
                error("Illegal transition for guide $guideId: $current -> $target")
            }
            runCallback(entry, target) { ctx -> block(ctx, entry.guide) }
        }
    }

    private suspend fun runCallback(
        entry: Entry,
        target: GuideState,
        body: suspend (GuideContext) -> Unit,
    ) {
        val ctx = GuideContext(entry.scope, eventBus, dispatchers, time)
        val previous = entry.state.value
        try {
            body(ctx)
            entry.state.value = target
            eventBus.publish(GuideStateChangedEvent(entry.guide.id, previous, target, time.nowMs()))
        } catch (t: Throwable) {
            // Do not catch CancellationException — let structured cancellation propagate.
            if (t is kotlinx.coroutines.CancellationException) throw t
            entry.state.value = GuideState.STOPPED
            eventBus.publish(GuideErrorEvent(entry.guide.id, target, t, time.nowMs()))
        }
    }

    /** Useful in tests to assert no leaked coroutines remain. */
    internal fun activeJobs(): List<Job> = entries.values.map { it.job }

    /** Spawn a fire-and-forget coroutine bound to a guide's scope. */
    fun launchInGuide(guideId: String, block: suspend CoroutineScope.() -> Unit): Job? =
        entries[guideId]?.scope?.launch(block = block)
}
