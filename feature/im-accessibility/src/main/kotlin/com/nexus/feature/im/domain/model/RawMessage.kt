package com.nexus.feature.im.domain.model

/**
 * A raw message extracted from an IM app's accessibility tree.
 *
 * @property packageName Source app package, e.g. "com.tencent.mm".
 * @property senderName Display name of the message sender.
 * @property content Message body text.
 * @property timestampMs Best-effort timestamp (usually System.currentTimeMillis()).
 * @property conversationHint Group name or chat title, if identifiable.
 * @property confidence Parsing confidence in [0.0, 1.0]. Messages below the
 *                      pipeline threshold are dropped.
 */
data class RawMessage(
    val packageName: String,
    val senderName: String,
    val content: String,
    val timestampMs: Long,
    val conversationHint: String? = null,
    val confidence: Float = 1.0f,
)
