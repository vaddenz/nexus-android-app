package com.nexus.android.harness.sensors

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.nexus.harness.sensor.Sensor
import com.nexus.harness.sensor.SensorKind
import com.nexus.harness.sensor.SensorPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Snapshot of the device battery at a point in time. */
data class BatteryReading(
    val levelPercent: Int,
    val charging: Boolean,
    val plugType: PlugType,
)

enum class PlugType { UNPLUGGED, AC, USB, WIRELESS, UNKNOWN }

/**
 * Polling sensor that samples battery level + charging state at a fixed cadence.
 *
 * Uses the sticky [Intent.ACTION_BATTERY_CHANGED] broadcast — registering a null
 * receiver returns the last sticky intent without leaking a receiver. This keeps
 * the sensor allocation-free at steady state.
 */
@Singleton
class BatterySensor @Inject constructor(
    @ApplicationContext private val context: Context,
) : Sensor<BatteryReading> {

    override val id = "battery"
    override val kind = SensorKind.SYSTEM
    override val policy = SensorPolicy.Polling(intervalMs = 30_000L)

    override suspend fun sample(): BatteryReading {
        val intent: Intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return BatteryReading(levelPercent = -1, charging = false, plugType = PlugType.UNKNOWN)
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level < 0 || scale <= 0) -1 else (level * 100) / scale
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val plug = when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
            BatteryManager.BATTERY_PLUGGED_AC -> PlugType.AC
            BatteryManager.BATTERY_PLUGGED_USB -> PlugType.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> PlugType.WIRELESS
            0 -> PlugType.UNPLUGGED
            else -> PlugType.UNKNOWN
        }
        return BatteryReading(percent, charging, plug)
    }
}
