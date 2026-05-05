package com.nexus.harness.sensor

import com.nexus.core.eventbus.Event

/**
 * Single observation produced by a [Sensor] and broadcast on the bus.
 *
 * The [value] is erased to [Any] because Kotlin generic parameters are not retained
 * at runtime; subscribers pattern-match on [sensorId] then cast [value] safely.
 */
data class SensorObservation(
    val sensorId: String,
    val value: Any,
    override val timestampMs: Long,
) : Event {
    /** Convenience cast helper. */
    inline fun <reified T> valueAs(): T = value as T
}

/** Categorizes a sensor for routing/filtering purposes. */
enum class SensorKind { SYSTEM, USER, ENVIRONMENT, BEHAVIORAL, DERIVED }

/** Lifecycle phases of an individual sensor. */
enum class SensorState { IDLE, ACTIVE, PAUSED, STOPPED }

/** Strategy describing how the scheduler should drive a sensor. */
sealed interface SensorPolicy {

    data object Reactive : SensorPolicy

    data class Polling(val intervalMs: Long) : SensorPolicy {
        init {
            require(intervalMs > 0) { "Polling interval must be positive, got $intervalMs" }
        }
    }
}
