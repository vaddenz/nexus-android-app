package com.nexus.android.harness.pipelines

import com.nexus.core.eventbus.EventBus
import com.nexus.core.eventbus.subscribe
import com.nexus.feature.im.domain.desensitizer.Desensitizer
import com.nexus.feature.im.domain.model.RawMessage
import com.nexus.harness.pipeline.DataContext
import com.nexus.harness.pipeline.Pipeline
import com.nexus.harness.sensor.SensorObservation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pipeline that subscribes to raw IM messages from the [im-accessibility] sensor,
 * applies desensitization rules, and publishes structured [ImMessageContext].
 */
@Singleton
class DesensitizationPipeline @Inject constructor(
    private val desensitizer: Desensitizer,
) : Pipeline<ImMessageContext> {

    override val id = "desensitization"

    override fun build(bus: EventBus): Flow<ImMessageContext> =
        bus.subscribe<SensorObservation>()
            .filter { it.sensorId == "im-accessibility" }
            .map { it.valueAs<RawMessage>() }
            .filter { it.confidence >= 0.4f }
            .map { msg ->
                val processed = desensitizer.process(msg)
                ImMessageContext(
                    pipelineId = id,
                    packageName = processed.packageName,
                    senderName = processed.senderName,
                    content = processed.content,
                    timestampMs = processed.timestampMs,
                    confidence = processed.confidence,
                    isDesensitized = processed.content != msg.content,
                )
            }
}

/**
 * Structured output of the desensitization pipeline.
 */
data class ImMessageContext(
    override val pipelineId: String,
    val packageName: String,
    val senderName: String,
    val content: String,
    override val timestampMs: Long,
    val confidence: Float,
    val isDesensitized: Boolean,
) : DataContext
