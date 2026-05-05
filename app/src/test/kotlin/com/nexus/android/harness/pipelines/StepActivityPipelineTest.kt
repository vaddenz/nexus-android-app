package com.nexus.android.harness.pipelines

import com.google.common.truth.Truth.assertThat
import com.nexus.android.harness.sensors.StepEvent
import com.nexus.core.eventbus.FlowEventBus
import com.nexus.harness.sensor.SensorObservation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StepActivityPipelineTest {

    @Test
    fun `classifies as VIGOROUS for high cadence`() = runTest {
        val bus = FlowEventBus(extraBufferCapacity = 256)
        val pipeline = StepActivityPipeline()
        val out = mutableListOf<StepActivityContext>()
        val collectJob = launch { pipeline.build(bus).take(1).toList(out) }
        advanceUntilIdle()
        // 200 steps in 60s -> 200 spm -> VIGOROUS.
        repeat(199) { i ->
            bus.publish(SensorObservation("step-cadence", StepEvent(0L), i * 300L))
        }
        bus.publish(SensorObservation("step-cadence", StepEvent(0L), 60_000L))
        advanceUntilIdle()
        collectJob.join()

        assertThat(out[0].level).isEqualTo(ActivityLevel.VIGOROUS)
        assertThat(out[0].stepsInWindow).isEqualTo(200)
    }

    @Test
    fun `classifies as IDLE for very low cadence`() = runTest {
        val bus = FlowEventBus(extraBufferCapacity = 64)
        val pipeline = StepActivityPipeline()
        val out = mutableListOf<StepActivityContext>()
        val collectJob = launch { pipeline.build(bus).take(1).toList(out) }
        advanceUntilIdle()
        bus.publish(SensorObservation("step-cadence", StepEvent(0L), 0L))
        bus.publish(SensorObservation("step-cadence", StepEvent(0L), 60_000L))
        advanceUntilIdle()
        collectJob.join()

        assertThat(out[0].level).isEqualTo(ActivityLevel.IDLE)
    }
}
