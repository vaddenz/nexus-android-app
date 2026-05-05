package com.nexus.core.eventbus

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Default [EventBus] backed by [MutableSharedFlow].
 *
 * @param replay Number of past events delivered to new subscribers (default 0 = pure hot stream).
 * @param extraBufferCapacity Slow-subscriber tolerance before publish suspends.
 * @param onBufferOverflow Behavior when buffer is full and a non-suspending publish hits.
 */
class FlowEventBus(
    replay: Int = 0,
    extraBufferCapacity: Int = 64,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,
) : EventBus {

    private val sink = MutableSharedFlow<Event>(
        replay = replay,
        extraBufferCapacity = extraBufferCapacity,
        onBufferOverflow = onBufferOverflow,
    )

    override val events: Flow<Event> = sink.asSharedFlow()

    override suspend fun publish(event: Event) {
        sink.emit(event)
    }

    override fun tryPublish(event: Event): Boolean = sink.tryEmit(event)

    /** Number of active subscribers — useful for diagnostics. */
    val subscriberCount: Int get() = sink.subscriptionCount.value
}
