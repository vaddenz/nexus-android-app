package com.nexus.harness.sensor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * A data source that produces [SensorObservation]s.
 *
 * Implementations choose between two delivery modes:
 *
 * - [SensorPolicy.Reactive]: override [observe] to expose a hot/cold [Flow]. The scheduler
 *   just collects from it. Use this for system callbacks (e.g. BroadcastReceivers).
 * - [SensorPolicy.Polling]: override [sample] to compute a single value. The scheduler
 *   ticks at the configured interval. Use this for state queries (e.g. battery level).
 */
interface Sensor<T : Any> {
    val id: String
    val kind: SensorKind
    val policy: SensorPolicy

    /** Reactive stream — invoked once per scheduling activation. Default = empty. */
    fun observe(): Flow<T> = emptyFlow()

    /** Pull a single sample — invoked every poll tick. Default throws to surface misuse. */
    suspend fun sample(): T = error("sample() must be overridden when policy is Polling")

    /** Optional warm-up; called once when the sensor first activates. */
    suspend fun onActivate() {}

    /** Optional teardown; called when the sensor is deactivated. */
    suspend fun onDeactivate() {}
}
