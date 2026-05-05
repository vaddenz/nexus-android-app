package com.nexus.core.eventbus

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance

/**
 * Thread-safe pub/sub bus for inter-module communication via Kotlin Flow.
 *
 * - Hot stream semantics: subscribers only receive events emitted after subscription.
 * - Type-safe filtering via [subscribe] inline reified function.
 * - No buffering by default; use [tryPublish] when caller cannot suspend.
 */
interface EventBus {

    suspend fun publish(event: Event)

    fun tryPublish(event: Event): Boolean

    val events: Flow<Event>
}

/** Type-safe subscription. Filters the stream to events of type [T] only. */
inline fun <reified T : Event> EventBus.subscribe(): Flow<T> =
    events.filterIsInstance()
