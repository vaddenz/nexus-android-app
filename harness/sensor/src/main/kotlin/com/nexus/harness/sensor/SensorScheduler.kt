package com.nexus.harness.sensor

import com.nexus.core.common.coroutines.DispatcherProvider
import com.nexus.core.common.time.TimeProvider
import com.nexus.core.eventbus.EventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Drives [Sensor]s registered in a [SensorRegistry] according to their [SensorPolicy].
 *
 * Each active sensor runs in its own supervisor scope. A sensor that throws does
 * not affect other sensors; the scheduler logs an error event on the bus and
 * marks the sensor [SensorState.STOPPED] so callers can choose to restart it.
 */
class SensorScheduler(
    private val parentScope: CoroutineScope,
    private val registry: SensorRegistry,
    private val eventBus: EventBus,
    private val dispatchers: DispatcherProvider,
    private val time: TimeProvider,
) {

    private data class Active(val job: Job, var state: SensorState)

    private val active = ConcurrentHashMap<String, Active>()

    fun stateOf(sensorId: String): SensorState =
        active[sensorId]?.state ?: SensorState.IDLE

    /**
     * Activate a registered sensor. Idempotent: returns false if already active.
     */
    fun activate(sensorId: String): Boolean {
        if (active.containsKey(sensorId)) return false
        val sensor = registry.get(sensorId) ?: return false
        val supervisor = SupervisorJob(parent = parentScope.coroutineContext[Job])
        val scope = CoroutineScope(parentScope.coroutineContext + supervisor + dispatchers.default)
        val job = scope.launch { drive(sensor) }
        active[sensorId] = Active(job, SensorState.ACTIVE)
        scope.launch {
            eventBus.publish(SensorLifecycleEvent(sensorId, SensorState.ACTIVE, time.nowMs()))
        }
        return true
    }

    /** Stop a sensor and cancel its scope. Idempotent. */
    suspend fun deactivate(sensorId: String) {
        val entry = active.remove(sensorId) ?: return
        entry.state = SensorState.STOPPED
        entry.job.cancel()
        registry.get(sensorId)?.onDeactivate()
        eventBus.publish(SensorLifecycleEvent(sensorId, SensorState.STOPPED, time.nowMs()))
    }

    /** Stop every active sensor. */
    suspend fun deactivateAll() {
        active.keys.toList().forEach { deactivate(it) }
    }

    private suspend fun <T : Any> drive(sensor: Sensor<T>) {
        try {
            sensor.onActivate()
            when (val policy = sensor.policy) {
                SensorPolicy.Reactive -> driveReactive(sensor)
                is SensorPolicy.Polling -> drivePolling(sensor, policy.intervalMs)
            }
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            eventBus.publish(SensorErrorEvent(sensor.id, t, time.nowMs()))
        }
    }

    private suspend fun <T : Any> driveReactive(sensor: Sensor<T>) {
        sensor.observe()
            .catch { t ->
                if (t !is kotlinx.coroutines.CancellationException) {
                    eventBus.publish(SensorErrorEvent(sensor.id, t, time.nowMs()))
                }
            }
            .collectLatest { value ->
                eventBus.publish(SensorObservation(sensor.id, value, time.nowMs()))
            }
    }

    private suspend fun <T : Any> drivePolling(sensor: Sensor<T>, intervalMs: Long) {
        while (kotlinx.coroutines.currentCoroutineContext()[Job]?.isActive == true) {
            try {
                val value = sensor.sample()
                eventBus.publish(SensorObservation(sensor.id, value, time.nowMs()))
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                eventBus.publish(SensorErrorEvent(sensor.id, t, time.nowMs()))
            }
            delay(intervalMs)
        }
    }
}

/** Bus event emitted on every sensor state transition. */
data class SensorLifecycleEvent(
    val sensorId: String,
    val state: SensorState,
    override val timestampMs: Long,
) : com.nexus.core.eventbus.Event

/** Bus event emitted when a sensor's [Sensor.observe] or [Sensor.sample] throws. */
data class SensorErrorEvent(
    val sensorId: String,
    val cause: Throwable,
    override val timestampMs: Long,
) : com.nexus.core.eventbus.Event
