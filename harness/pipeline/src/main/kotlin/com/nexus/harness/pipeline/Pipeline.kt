package com.nexus.harness.pipeline

import com.nexus.core.eventbus.EventBus
import kotlinx.coroutines.flow.Flow

/**
 * A data-context producer. Builds a cold [Flow] from raw bus events that, when
 * collected, emits derived [DataContext] values.
 *
 * Pipelines must be pure (no side effects) and idempotent — the [PipelineRunner]
 * may build them multiple times. All side effects happen via the runner.
 */
interface Pipeline<C : DataContext> {
    val id: String

    /** Build the pipeline's output flow given a bus to subscribe to. */
    fun build(bus: EventBus): Flow<C>
}
