package com.nexus.harness.pipeline

import com.nexus.core.eventbus.Event
import com.nexus.core.eventbus.EventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Runs registered [Pipeline]s and republishes their outputs onto the [EventBus].
 *
 * Each running pipeline owns its own supervisor scope; failures are isolated and
 * surfaced as [PipelineErrorEvent]. Calling [stop] cancels every running pipeline.
 */
class PipelineRunner(
    private val parentScope: CoroutineScope,
    private val registry: PipelineRegistry,
    private val eventBus: EventBus,
) {

    private val jobs = ConcurrentHashMap<String, Job>()

    /** @return false if the pipeline is already running or unknown. */
    fun start(pipelineId: String): Boolean {
        if (jobs.containsKey(pipelineId)) return false
        val pipeline = registry.get(pipelineId) ?: return false
        val supervisor = SupervisorJob(parent = parentScope.coroutineContext[Job])
        val scope = CoroutineScope(parentScope.coroutineContext + supervisor)
        val job = scope.launch { run(pipeline) }
        jobs[pipelineId] = job
        return true
    }

    /** Start every registered pipeline. */
    fun startAll() {
        registry.all().forEach { start(it.id) }
    }

    /** Stop a single pipeline. Idempotent. */
    fun stop(pipelineId: String): Boolean {
        val job = jobs.remove(pipelineId) ?: return false
        job.cancel()
        return true
    }

    /** Stop every running pipeline. */
    fun stopAll() {
        jobs.keys.toList().forEach { stop(it) }
    }

    fun isRunning(pipelineId: String): Boolean = jobs.containsKey(pipelineId)

    private suspend fun run(pipeline: Pipeline<*>) {
        pipeline.build(eventBus)
            .catch { t ->
                if (t !is kotlinx.coroutines.CancellationException) {
                    eventBus.publish(PipelineErrorEvent(pipeline.id, t, System.currentTimeMillis()))
                }
            }
            .collect { ctx ->
                eventBus.publish(ctx)
            }
    }
}

data class PipelineErrorEvent(
    val pipelineId: String,
    val cause: Throwable,
    override val timestampMs: Long,
) : Event
