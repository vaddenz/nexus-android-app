package com.nexus.harness.guide

/** Lifecycle phases a [Guide] passes through. Linear with two ends: [DESTROYED] is terminal. */
enum class GuideState {
    INITIALIZED,
    STARTED,
    RUNNING,
    PAUSED,
    STOPPED,
    DESTROYED,
    ;

    /** Transition guard: returns true if [target] is a valid next state from this state. */
    fun canTransitionTo(target: GuideState): Boolean = when (this) {
        INITIALIZED -> target == STARTED || target == DESTROYED
        STARTED -> target == RUNNING || target == STOPPED || target == DESTROYED
        RUNNING -> target == PAUSED || target == STOPPED || target == DESTROYED
        PAUSED -> target == RUNNING || target == STOPPED || target == DESTROYED
        STOPPED -> target == STARTED || target == DESTROYED
        DESTROYED -> false
    }
}
