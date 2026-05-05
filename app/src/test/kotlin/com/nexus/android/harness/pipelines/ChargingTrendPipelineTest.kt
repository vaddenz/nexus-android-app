package com.nexus.android.harness.pipelines

import com.google.common.truth.Truth.assertThat
import com.nexus.android.harness.sensors.BatteryReading
import com.nexus.android.harness.sensors.PlugType
import com.nexus.core.eventbus.FlowEventBus
import com.nexus.core.eventbus.subscribe
import com.nexus.harness.sensor.SensorObservation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChargingTrendPipelineTest {

    private fun reading(level: Int, charging: Boolean = false) =
        BatteryReading(level, charging, PlugType.UNPLUGGED)

    @Test
    fun `emits CHARGING when level rises by threshold`() = runTest {
        val bus = FlowEventBus(extraBufferCapacity = 16)
        val pipeline = ChargingTrendPipeline()
        val out = mutableListOf<ChargingTrendContext>()
        val collectJob = launch {
            pipeline.build(bus).take(1).toList(out)
        }
        advanceUntilIdle()
        bus.publish(SensorObservation("battery", reading(50), 1000))
        bus.publish(SensorObservation("battery", reading(52), 2000))
        bus.publish(SensorObservation("battery", reading(54), 3000))
        advanceUntilIdle()
        collectJob.join()

        assertThat(out).hasSize(1)
        assertThat(out[0].trend).isEqualTo(ChargingTrend.CHARGING)
        assertThat(out[0].deltaPercent).isEqualTo(4)
    }

    @Test
    fun `emits DRAINING when level drops`() = runTest {
        val bus = FlowEventBus(extraBufferCapacity = 16)
        val pipeline = ChargingTrendPipeline()
        val out = mutableListOf<ChargingTrendContext>()
        val collectJob = launch { pipeline.build(bus).take(1).toList(out) }
        advanceUntilIdle()
        bus.publish(SensorObservation("battery", reading(80), 100))
        bus.publish(SensorObservation("battery", reading(78), 200))
        bus.publish(SensorObservation("battery", reading(75), 300))
        advanceUntilIdle()
        collectJob.join()

        assertThat(out[0].trend).isEqualTo(ChargingTrend.DRAINING)
        assertThat(out[0].deltaPercent).isEqualTo(-5)
    }

    @Test
    fun `emits STABLE when delta is small`() = runTest {
        val bus = FlowEventBus(extraBufferCapacity = 16)
        val pipeline = ChargingTrendPipeline()
        val out = mutableListOf<ChargingTrendContext>()
        val collectJob = launch { pipeline.build(bus).take(1).toList(out) }
        advanceUntilIdle()
        bus.publish(SensorObservation("battery", reading(50), 1))
        bus.publish(SensorObservation("battery", reading(51), 2))
        bus.publish(SensorObservation("battery", reading(50), 3))
        advanceUntilIdle()
        collectJob.join()

        assertThat(out[0].trend).isEqualTo(ChargingTrend.STABLE)
    }

    @Test
    fun `emits UNKNOWN when battery level is unavailable`() = runTest {
        val bus = FlowEventBus(extraBufferCapacity = 16)
        val pipeline = ChargingTrendPipeline()
        val out = mutableListOf<ChargingTrendContext>()
        val collectJob = launch { pipeline.build(bus).take(1).toList(out) }
        advanceUntilIdle()
        bus.publish(SensorObservation("battery", reading(-1), 1))
        bus.publish(SensorObservation("battery", reading(50), 2))
        bus.publish(SensorObservation("battery", reading(52), 3))
        advanceUntilIdle()
        collectJob.join()

        assertThat(out[0].trend).isEqualTo(ChargingTrend.UNKNOWN)
    }

    @Test
    fun `ignores observations from other sensors`() = runTest {
        val bus = FlowEventBus(extraBufferCapacity = 16)
        val pipeline = ChargingTrendPipeline()
        val out = mutableListOf<ChargingTrendContext>()
        val collectJob = launch { pipeline.build(bus).take(1).toList(out) }
        advanceUntilIdle()
        bus.publish(SensorObservation("network", "noise", 1))
        bus.publish(SensorObservation("battery", reading(50), 1))
        bus.publish(SensorObservation("network", "noise", 2))
        bus.publish(SensorObservation("battery", reading(52), 2))
        bus.publish(SensorObservation("battery", reading(54), 3))
        advanceUntilIdle()
        collectJob.join()

        assertThat(out).hasSize(1)
        assertThat(out[0].trend).isEqualTo(ChargingTrend.CHARGING)
    }
}
