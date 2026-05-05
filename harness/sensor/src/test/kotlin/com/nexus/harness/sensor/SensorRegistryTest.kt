package com.nexus.harness.sensor

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SensorRegistryTest {

    private fun stubSensor(id: String, kind: SensorKind = SensorKind.SYSTEM): Sensor<Int> =
        object : Sensor<Int> {
            override val id: String = id
            override val kind: SensorKind = kind
            override val policy: SensorPolicy = SensorPolicy.Polling(100L)
            override suspend fun sample(): Int = 0
        }

    @Test
    fun `register returns true for new sensor`() {
        val registry = SensorRegistry()
        assertThat(registry.register(stubSensor("a"))).isTrue()
        assertThat(registry.size()).isEqualTo(1)
    }

    @Test
    fun `register returns false for duplicate id`() {
        val registry = SensorRegistry()
        registry.register(stubSensor("a"))
        assertThat(registry.register(stubSensor("a"))).isFalse()
        assertThat(registry.size()).isEqualTo(1)
    }

    @Test
    fun `unregister removes and returns sensor`() {
        val registry = SensorRegistry()
        registry.register(stubSensor("a"))
        val removed = registry.unregister("a")
        assertThat(removed).isNotNull()
        assertThat(registry.size()).isEqualTo(0)
    }

    @Test
    fun `unregister missing returns null`() {
        val registry = SensorRegistry()
        assertThat(registry.unregister("missing")).isNull()
    }

    @Test
    fun `byKind filters correctly`() {
        val registry = SensorRegistry()
        registry.register(stubSensor("sys-1", SensorKind.SYSTEM))
        registry.register(stubSensor("sys-2", SensorKind.SYSTEM))
        registry.register(stubSensor("env-1", SensorKind.ENVIRONMENT))

        assertThat(registry.byKind(SensorKind.SYSTEM).map { it.id })
            .containsExactly("sys-1", "sys-2")
        assertThat(registry.byKind(SensorKind.ENVIRONMENT).map { it.id })
            .containsExactly("env-1")
        assertThat(registry.byKind(SensorKind.USER)).isEmpty()
    }

    @Test
    fun `polling policy rejects non-positive interval`() {
        val ex = assertThrows<IllegalArgumentException> {
            SensorPolicy.Polling(0L)
        }
        assertThat(ex).hasMessageThat().contains("must be positive")
    }

    private inline fun <reified T : Throwable> assertThrows(block: () -> Unit): T {
        try { block() } catch (t: Throwable) { if (t is T) return t else throw t }
        error("Expected ${T::class.simpleName}")
    }
}
