package com.nexus.android.harness

import com.nexus.android.harness.guides.EpisodicMemoryRecorderGuide
import com.nexus.android.harness.pipelines.ChargingTrendPipeline
import com.nexus.android.harness.pipelines.DesensitizationPipeline
import com.nexus.android.harness.pipelines.StepActivityPipeline
import com.nexus.android.harness.sensors.BatterySensor
import com.nexus.android.harness.sensors.ImAccessibilitySensor
import com.nexus.android.harness.sensors.NetworkSensor
import com.nexus.android.harness.sensors.StepCadenceSensor
import com.nexus.core.common.coroutines.DispatcherProvider
import com.nexus.core.common.time.TimeProvider
import com.nexus.core.eventbus.EventBus
import com.nexus.harness.guide.GuideContext
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
 * - Register sensors and pipelines.
 * - Activate sensors and start pipelines with a shared application scope.
 * - Start lifecycle guides (e.g. episodic memory recorder).
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
    private val imAccessibilitySensor: ImAccessibilitySensor,
    private val chargingTrend: ChargingTrendPipeline,
    private val stepActivity: StepActivityPipeline,
    private val desensitizationPipeline: DesensitizationPipeline,
    private val memoryRecorderGuide: EpisodicMemoryRecorderGuide,
) {

    private var scheduler: SensorScheduler? = null
    private var runner: PipelineRunner? = null
    private var harnessScope: CoroutineScope? = null

    fun bootstrap(scope: CoroutineScope) {
        if (scheduler != null) return // idempotent
        harnessScope = scope

        // Register sensors
        sensorRegistry.register(batterySensor)
        sensorRegistry.register(networkSensor)
        sensorRegistry.register(stepSensor)
        sensorRegistry.register(imAccessibilitySensor)

        // Register pipelines
        pipelineRegistry.register(chargingTrend)
        pipelineRegistry.register(stepActivity)
        pipelineRegistry.register(desensitizationPipeline)

        val sched = SensorScheduler(scope, sensorRegistry, eventBus, dispatchers, time)
        val run = PipelineRunner(scope, pipelineRegistry, eventBus)
        scheduler = sched
        runner = run

        run.startAll()
        sched.activate(batterySensor.id)
        sched.activate(networkSensor.id)
        sched.activate(stepSensor.id)
        sched.activate(imAccessibilitySensor.id)

        // Start memory recorder guide
        val guideCtx = GuideContext(
            scope = scope,
            eventBus = eventBus,
            dispatchers = dispatchers,
            time = time,
        )
        scope.launch {
            memoryRecorderGuide.onRun(guideCtx)
        }
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
