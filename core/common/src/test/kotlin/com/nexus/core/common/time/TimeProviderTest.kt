package com.nexus.core.common.time

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TimeProviderTest {

    @Test
    fun `system time provider returns current time`() {
        val before = System.currentTimeMillis()
        val now = SystemTimeProvider.nowMs()
        val after = System.currentTimeMillis()
        assertThat(now).isAtLeast(before)
        assertThat(now).isAtMost(after)
    }

    @Test
    fun `fun interface allows lambda construction`() {
        val fixed = TimeProvider { 12345L }
        assertThat(fixed.nowMs()).isEqualTo(12345L)
    }
}
