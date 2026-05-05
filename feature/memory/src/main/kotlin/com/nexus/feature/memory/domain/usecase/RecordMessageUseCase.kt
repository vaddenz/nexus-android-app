package com.nexus.feature.memory.domain.usecase

import com.nexus.feature.memory.domain.model.EpisodicEvent
import com.nexus.feature.memory.domain.repository.EpisodicMemoryRepository
import javax.inject.Inject

/**
 * Records a desensitized IM message into episodic memory.
 */
class RecordMessageUseCase @Inject constructor(
    private val repository: EpisodicMemoryRepository,
) {
    suspend operator fun invoke(
        packageName: String,
        senderName: String,
        content: String,
        confidence: Float,
        isDesensitized: Boolean,
    ) {
        val now = System.currentTimeMillis()
        repository.record(
            EpisodicEvent(
                id = java.util.UUID.randomUUID().toString(),
                eventType = "im_message",
                sourcePackage = packageName,
                senderName = senderName,
                contentText = content,
                occurredAt = now,
                collectedAt = now,
                confidence = confidence,
                isDesensitized = isDesensitized,
            )
        )
    }
}
