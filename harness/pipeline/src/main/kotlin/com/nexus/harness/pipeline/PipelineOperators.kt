package com.nexus.harness.pipeline

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Emits a list every [size] upstream emissions (tumbling window).
 * Partial windows at the end of the upstream are dropped.
 */
fun <T> Flow<T>.windowedTumbling(size: Int): Flow<List<T>> {
    require(size > 0) { "Window size must be positive, got $size" }
    return flow {
        val buffer = ArrayDeque<T>(size)
        collect { value ->
            buffer.addLast(value)
            if (buffer.size == size) {
                emit(buffer.toList())
                buffer.clear()
            }
        }
    }
}

/**
 * Sliding window of [size] elements, emitting on every new element after the
 * window is filled.
 */
fun <T> Flow<T>.windowedSliding(size: Int): Flow<List<T>> {
    require(size > 0) { "Window size must be positive, got $size" }
    return flow {
        val buffer = ArrayDeque<T>(size)
        collect { value ->
            buffer.addLast(value)
            if (buffer.size > size) buffer.removeFirst()
            if (buffer.size == size) emit(buffer.toList())
        }
    }
}
