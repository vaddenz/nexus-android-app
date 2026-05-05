package com.nexus.android.harness.pipelines

import com.nexus.android.harness.sensors.BatteryReading
import com.nexus.core.eventbus.EventBus
import com.nexus.core.eventbus.subscribe
import com.nexus.harness.pipeline.DataContext
import com.nexus.harness.pipeline.Pipeline
import com.nexus.harness.pipeline.windowedSliding
import com.nexus.harness.sensor.SensorObservation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ChargingTrend { CHARGING, DRAINING, STABLE, UNKNOWN }

data class ChargingTrendContext(
    val trend: ChargingTrend,
    val deltaPercent: Int,
    val windowMs: Long,
    override val pipelineId: String,
    override val timestampMs: Long,
) : DataContext

/**
 * Derives a [ChargingTrend] from a sliding window of three [BatteryReading]s.
 *
 * The +2 / -2 percent deltas are rough thresholds: small fluctuations from
 * thermal calibration are ignored as STABLE.
 */
@Singleton
class ChargingTrendPipeline @Inject constructor() : Pipeline<ChargingTrendContext> {

    override val id = "charging-trend"

    override fun build(bus: EventBus): Flow<ChargingTrendContext> =
        bus.subscribe<SensorObservation>()
            .filter { it.sensorId == "battery" && it.value is BatteryReading }
            .map { obs -> obs.timestampMs to (obs.value as BatteryReading) }
            .windowedSliding(WINDOW_SIZE)
            .map { window ->
                val first = window.first()
                val last = window.last()
                val delta = last.second.levelPercent - first.second.levelPercent
                val trend = when {
                    last.second.levelPercent < 0 || first.second.levelPercent < 0 -> ChargingTrend.UNKNOWN
                    delta >= CHARGING_THRESHOLD -> ChargingTrend.CHARGING
                    delta <= -CHARGING_THRESHOLD -> ChargingTrend.DRAINING
                    else -> ChargingTrend.STABLE
                }
                ChargingTrendContext(
                    trend = trend,
                    deltaPercent = delta,
                    windowMs = last.first - first.first,
                    pipelineId = id,
                    timestampMs = last.first,
                )
            }

    private companion object {
        const val WINDOW_SIZE = 3
        const val CHARGING_THRESHOLD = 2
    }
}
