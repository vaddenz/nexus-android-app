package com.nexus.core.eventbus

/**
 * Marker interface for all events flowing through the [EventBus].
 *
 * Events should be immutable data classes carrying observation values.
 * Implementations must be safe to publish across thread boundaries.
 */
interface Event {
    /** Monotonic timestamp in milliseconds when the event was produced. */
    val timestampMs: Long
}
