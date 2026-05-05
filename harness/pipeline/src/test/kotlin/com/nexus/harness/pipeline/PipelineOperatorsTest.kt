package com.nexus.harness.pipeline

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PipelineOperatorsTest {

    @Test
    fun `windowedTumbling chunks into fixed-size lists`() = runTest {
        val out = flowOf(1, 2, 3, 4, 5, 6).windowedTumbling(2).toList()
        assertThat(out).containsExactly(listOf(1, 2), listOf(3, 4), listOf(5, 6)).inOrder()
    }

    @Test
    fun `windowedTumbling drops partial tail window`() = runTest {
        val out = flowOf(1, 2, 3, 4, 5).windowedTumbling(2).toList()
        assertThat(out).containsExactly(listOf(1, 2), listOf(3, 4)).inOrder()
    }

    @Test
    fun `windowedSliding emits after window fills`() = runTest {
        val out = flowOf(1, 2, 3, 4).windowedSliding(2).toList()
        assertThat(out).containsExactly(listOf(1, 2), listOf(2, 3), listOf(3, 4)).inOrder()
    }

    @Test
    fun `windowedSliding emits nothing when input shorter than window`() = runTest {
        val out = flowOf(1).windowedSliding(3).toList()
        assertThat(out).isEmpty()
    }

    @Test
    fun `windowedTumbling rejects non-positive size`() {
        try {
            flowOf<Int>().windowedTumbling(0)
            error("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertThat(e).hasMessageThat().contains("must be positive")
        }
    }
}
