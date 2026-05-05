package com.nexus.feature.im.domain.desensitizer

import com.nexus.feature.im.domain.model.RawMessage

/**
 * Applies privacy-preserving transformations to a [RawMessage].
 *
 * Implementations must be pure (no side effects) and thread-safe.
 */
interface Desensitizer {
    fun process(message: RawMessage): RawMessage
}
