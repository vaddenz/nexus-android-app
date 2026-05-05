package com.nexus.android.harness

import com.nexus.android.harness.pipelines.ChargingTrendPipeline
import com.nexus.android.harness.pipelines.StepActivityPipeline
import com.nexus.android.harness.sensors.BatterySensor
import com.nexus.android.harness.sensors.NetworkSensor
import com.nexus.android.harness.sensors.StepCadenceSensor
import com.nexus.core.common.coroutines.DispatcherProvider
import com.nexus.core.common.time.TimeProvider
import com.nexus.core.eventbus.EventBus
import com.nexus.harness.pipeline.PipelineRegistry
import com.nexus.harness.pipeline.PipelineRunner
import com.nexus.harness.sensor.SensorRegistry
import com.nexus.harness.sensor.SensorScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Top-level orchestrator that bootstraps the harness on app start.
 *
 * Responsibilities:
 * - Register the three sample sensors and two pipelines.
 * - Activate sensors and start pipelines with a shared application scope.
 * - Provide a clean [shutdown] hook so tests and lifecycle owners can verify
 *   that cancellation propagates correctly (no leaked coroutines).
 */
@Singleton
class NexusHarness @Inject constructor(
    private val sensorRegistry: SensorRegistry,
    private val pipelineRegistry: PipelineRegistry,
    private val eventBus: EventBus,
    private val dispatchers: DispatcherProvider,
    private val time: TimeProvider,
    private val batterySensor: BatterySensor,
    private val networkSensor: NetworkSensor,
    private val stepSensor: StepCadenceSensor,
    private val chargingTrend: ChargingTrendPipeline,
    private val stepActivity: StepActivityPipeline,
) {

    private var scheduler: SensorScheduler? = null
    private var runner: PipelineRunner? = null
    private var harnessScope: CoroutineScope? = null

    fun bootstrap(scope: CoroutineScope) {
        if (scheduler != null) return // idempotent
        harnessScope = scope

        sensorRegistry.register(batterySensor)
        sensorRegistry.register(networkSensor)
        sensorRegistry.register(stepSensor)

        pipelineRegistry.register(chargingTrend)
        pipelineRegistry.register(stepActivity)

        val sched = SensorScheduler(scope, sensorRegistry, eventBus, dispatchers, time)
        val run = PipelineRunner(scope, pipelineRegistry, eventBus)
        scheduler = sched
        runner = run

        run.startAll()
        sched.activate(batterySensor.id)
        sched.activate(networkSensor.id)
        sched.activate(stepSensor.id)
    }

    fun shutdown() {
        val s = scheduler ?: return
        val r = runner ?: return
        val scope = harnessScope ?: return
        scope.launch {
            s.deactivateAll()
            r.stopAll()
        }
        scheduler = null
        runner = null
    }
}
