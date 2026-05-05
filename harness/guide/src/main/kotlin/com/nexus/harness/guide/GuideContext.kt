package com.nexus.harness.guide

import com.nexus.core.common.coroutines.DispatcherProvider
import com.nexus.core.common.time.TimeProvider
import com.nexus.core.eventbus.EventBus
import kotlinx.coroutines.CoroutineScope

/**
 * Runtime context handed to a [Guide] on its lifecycle callbacks.
 *
 * The [scope] is owned by the manager and cancelled when the guide is destroyed,
 * so any long-running work launched in it is automatically cleaned up — this is
 * the primary lifecycle-safety guarantee of the harness.
 */
data class GuideContext(
    val scope: CoroutineScope,
    val eventBus: EventBus,
    val dispatchers: DispatcherProvider,
    val time: TimeProvider,
)
