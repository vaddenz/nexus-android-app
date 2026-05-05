package com.nexus.android.harness.pipelines

import com.nexus.android.harness.sensors.StepEvent
import com.nexus.core.eventbus.EventBus
import com.nexus.core.eventbus.subscribe
import com.nexus.harness.pipeline.DataContext
import com.nexus.harness.pipeline.Pipeline
import com.nexus.harness.sensor.SensorObservation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

enum class ActivityLevel { IDLE, LIGHT, MODERATE, VIGOROUS }

data class StepActivityContext(
    val stepsInWindow: Int,
    val cadenceStepsPerMin: Float,
    val level: ActivityLevel,
    override val pipelineId: String,
    override val timestampMs: Long,
) : DataContext

/**
 * Aggregates step events into activity levels over a fixed [WINDOW_MS] window.
 *
 * Cadence thresholds follow common pedometer heuristics:
 * - <30 steps/min  → IDLE
 * - <80 steps/min  → LIGHT
 * - <120 steps/min → MODERATE
 * - else           → VIGOROUS
 */
@Singleton
class StepActivityPipeline @Inject constructor() : Pipeline<StepActivityContext> {

    override val id = "step-activity"

    override fun build(bus: EventBus): Flow<StepActivityContext> = flow {
        var windowStart = -1L
        var count = 0

        bus.subscribe<SensorObservation>()
            .filter { it.sensorId == "step-cadence" && it.value is StepEvent }
            .buffer(capacity = 64)
            .collect { obs ->
                if (windowStart < 0) windowStart = obs.timestampMs
                count++
                val elapsed = obs.timestampMs - windowStart
                if (elapsed >= WINDOW_MS) {
                    val cadence = (count.toFloat() / elapsed) * 60_000f
                    emit(
                        StepActivityContext(
                            stepsInWindow = count,
                            cadenceStepsPerMin = cadence,
                            level = classify(cadence),
                            pipelineId = id,
                            timestampMs = obs.timestampMs,
                        )
                    )
                    windowStart = obs.timestampMs
                    count = 0
                }
            }
    }

    private fun classify(cadence: Float): ActivityLevel = when {
        cadence < 30f -> ActivityLevel.IDLE
        cadence < 80f -> ActivityLevel.LIGHT
        cadence < 120f -> ActivityLevel.MODERATE
        else -> ActivityLevel.VIGOROUS
    }

    private companion object {
        const val WINDOW_MS = 60_000L
    }
}
