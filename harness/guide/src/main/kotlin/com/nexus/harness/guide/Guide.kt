package com.nexus.harness.guide

/**
 * A self-contained behavior the harness can drive through a lifecycle.
 *
 * Implementations should be lifecycle-safe: never retain references to
 * [GuideContext.scope] beyond [onDestroy], and launch all work through that scope.
 */
interface Guide {
    /** Stable, globally-unique identifier. */
    val id: String

    /** Human-readable label for diagnostics. */
    val name: String

    /** Called once after registration, before [onStart]. */
    suspend fun onInitialize(context: GuideContext) {}

    /** Called when the guide is asked to start. After return the guide is [GuideState.STARTED]. */
    suspend fun onStart(context: GuideContext) {}

    /** Called when the guide enters [GuideState.RUNNING] — main work happens here. */
    suspend fun onRun(context: GuideContext) {}

    /** Pause work but keep state. Return promptly. */
    suspend fun onPause(context: GuideContext) {}

    /** Resume work after a pause. */
    suspend fun onResume(context: GuideContext) {}

    /** Stop work. After return the guide may be re-started later. */
    suspend fun onStop(context: GuideContext) {}

    /** Terminal cleanup. After return, the guide must release all resources. */
    suspend fun onDestroy(context: GuideContext) {}
}
