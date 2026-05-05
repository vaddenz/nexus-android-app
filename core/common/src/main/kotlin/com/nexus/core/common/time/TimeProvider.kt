package com.nexus.core.common.time

/** Abstraction over the system clock for deterministic testing. */
fun interface TimeProvider {
    /** Wall-clock milliseconds since epoch. */
    fun nowMs(): Long
}

object SystemTimeProvider : TimeProvider {
    override fun nowMs(): Long = System.currentTimeMillis()
}
