package com.nexus.android.harness.sensors

import com.nexus.feature.im.domain.model.RawMessage
import com.nexus.feature.im.service.bridge.ImMessageBridge
import com.nexus.harness.sensor.Sensor
import com.nexus.harness.sensor.SensorKind
import com.nexus.harness.sensor.SensorPolicy
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Harness [Sensor] that observes IM messages captured by [ImAccessibilityService].
 *
 * Uses a [Reactive] policy because data is pushed by the system service,
 * not polled on an interval.
 */
@Singleton
class ImAccessibilitySensor @Inject constructor(
    private val bridge: ImMessageBridge,
) : Sensor<RawMessage> {

    override val id = "im-accessibility"
    override val kind = SensorKind.BEHAVIORAL
    override val policy = SensorPolicy.Reactive

    override fun observe(): Flow<RawMessage> = bridge.messages
}
