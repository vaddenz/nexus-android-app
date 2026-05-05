package com.nexus.android.harness.sensors

import android.content.Context
import android.hardware.Sensor as HwSensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.nexus.harness.sensor.Sensor
import com.nexus.harness.sensor.SensorKind
import com.nexus.harness.sensor.SensorPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

/** A discrete step detection event. */
data class StepEvent(val timestampNs: Long)

/**
 * Reactive sensor wrapping [HwSensor.TYPE_STEP_DETECTOR].
 *
 * Step detector emits one event per step with low latency, unlike step counter
 * which is monotonically cumulative. Some devices do not have this hardware —
 * in that case [observe] returns an empty flow and the sensor never emits.
 *
 * Note: requires the ACTIVITY_RECOGNITION permission at runtime on API 29+.
 */
@Singleton
class StepCadenceSensor @Inject constructor(
    @ApplicationContext private val context: Context,
) : Sensor<StepEvent> {

    override val id = "step-cadence"
    override val kind = SensorKind.BEHAVIORAL
    override val policy = SensorPolicy.Reactive

    private val manager: SensorManager
        get() = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    override fun observe(): Flow<StepEvent> {
        val hw = manager.getDefaultSensor(HwSensor.TYPE_STEP_DETECTOR) ?: return emptyFlow()
        return callbackFlow {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    trySend(StepEvent(timestampNs = event.timestamp))
                }
                override fun onAccuracyChanged(sensor: HwSensor?, accuracy: Int) = Unit
            }
            manager.registerListener(listener, hw, SensorManager.SENSOR_DELAY_NORMAL)
            awaitClose { manager.unregisterListener(listener) }
        }
    }
}
