package com.nexus.harness.pipeline

import com.nexus.core.eventbus.Event

/**
 * Result of a pipeline computation. Implementations are typed records that
 * downstream consumers (guides, UI) can pattern-match on.
 */
interface DataContext : Event {
    /** Identifier of the pipeline that produced this context. */
    val pipelineId: String
}
