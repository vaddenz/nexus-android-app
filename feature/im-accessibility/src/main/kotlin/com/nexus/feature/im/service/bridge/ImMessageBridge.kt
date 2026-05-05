package com.nexus.feature.im.service.bridge

import com.nexus.feature.im.domain.model.RawMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A thread-safe bridge that decouples [android.accessibilityservice.AccessibilityService]
 * (system lifecycle) from [com.nexus.harness.sensor.Sensor] (Hilt singleton).
 *
 * The service publishes raw messages here; the sensor observes them.
 */
@Singleton
class ImMessageBridge @Inject constructor() {

    private val _messages = MutableSharedFlow<RawMessage>(
        extraBufferCapacity = 128,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST,
    )

    val messages: SharedFlow<RawMessage> = _messages.asSharedFlow()

    fun emit(message: RawMessage): Boolean = _messages.tryEmit(message)
}
