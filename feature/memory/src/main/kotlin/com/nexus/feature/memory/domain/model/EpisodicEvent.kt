package com.nexus.feature.memory.domain.model

/**
 * Domain model for an event stored in episodic memory.
 *
 * @property id Unique identifier.
 * @property eventType Event category, e.g. "im_message".
 * @property sourcePackage Origin app package.
 * @property senderName Sender display name (empty for non-IM events).
 * @property contentText Event content / message body.
 * @property occurredAt Best-effort occurrence timestamp.
 * @property collectedAt Timestamp when the event was captured.
 * @property confidence Parsing confidence in [0.0, 1.0].
 * @property isDesensitized Whether sensitive data has been masked.
 */
data class EpisodicEvent(
    val id: String,
    val eventType: String,
    val sourcePackage: String,
    val senderName: String,
    val contentText: String,
    val occurredAt: Long,
    val collectedAt: Long,
    val confidence: Float,
    val isDesensitized: Boolean,
)
