package com.nexus.harness.guide

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GuideStateTest {

    @Test
    fun `INITIALIZED can transition to STARTED or DESTROYED`() {
        assertThat(GuideState.INITIALIZED.canTransitionTo(GuideState.STARTED)).isTrue()
        assertThat(GuideState.INITIALIZED.canTransitionTo(GuideState.DESTROYED)).isTrue()
        assertThat(GuideState.INITIALIZED.canTransitionTo(GuideState.RUNNING)).isFalse()
        assertThat(GuideState.INITIALIZED.canTransitionTo(GuideState.PAUSED)).isFalse()
    }

    @Test
    fun `RUNNING can transition to PAUSED, STOPPED, or DESTROYED`() {
        assertThat(GuideState.RUNNING.canTransitionTo(GuideState.PAUSED)).isTrue()
        assertThat(GuideState.RUNNING.canTransitionTo(GuideState.STOPPED)).isTrue()
        assertThat(GuideState.RUNNING.canTransitionTo(GuideState.DESTROYED)).isTrue()
        assertThat(GuideState.RUNNING.canTransitionTo(GuideState.STARTED)).isFalse()
        assertThat(GuideState.RUNNING.canTransitionTo(GuideState.INITIALIZED)).isFalse()
    }

    @Test
    fun `PAUSED can resume to RUNNING or stop`() {
        assertThat(GuideState.PAUSED.canTransitionTo(GuideState.RUNNING)).isTrue()
        assertThat(GuideState.PAUSED.canTransitionTo(GuideState.STOPPED)).isTrue()
    }

    @Test
    fun `STOPPED can restart or be destroyed`() {
        assertThat(GuideState.STOPPED.canTransitionTo(GuideState.STARTED)).isTrue()
        assertThat(GuideState.STOPPED.canTransitionTo(GuideState.DESTROYED)).isTrue()
        assertThat(GuideState.STOPPED.canTransitionTo(GuideState.RUNNING)).isFalse()
    }

    @Test
    fun `DESTROYED is terminal`() {
        GuideState.entries.forEach { target ->
            assertThat(GuideState.DESTROYED.canTransitionTo(target)).isFalse()
        }
    }
}
