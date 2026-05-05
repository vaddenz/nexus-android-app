package com.nexus.harness.guide

import com.nexus.core.eventbus.Event

/** Emitted on every guide lifecycle transition. */
data class GuideStateChangedEvent(
    val guideId: String,
    val from: GuideState,
    val to: GuideState,
    override val timestampMs: Long,
) : Event

/** Emitted when a guide callback throws. The harness recovers by transitioning to [GuideState.STOPPED]. */
data class GuideErrorEvent(
    val guideId: String,
    val state: GuideState,
    val cause: Throwable,
    override val timestampMs: Long,
) : Event
