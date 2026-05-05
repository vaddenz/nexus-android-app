package com.nexus.harness.sensor

import java.util.concurrent.ConcurrentHashMap

/** Type-safe registry of [Sensor] implementations keyed by their id. */
class SensorRegistry {

    private val sensors = ConcurrentHashMap<String, Sensor<*>>()

    /** @return false if a sensor with the same id is already registered. */
    fun register(sensor: Sensor<*>): Boolean =
        sensors.putIfAbsent(sensor.id, sensor) == null

    /** @return the removed sensor, or null if not registered. */
    fun unregister(sensorId: String): Sensor<*>? = sensors.remove(sensorId)

    fun get(sensorId: String): Sensor<*>? = sensors[sensorId]

    fun all(): List<Sensor<*>> = sensors.values.toList()

    fun byKind(kind: SensorKind): List<Sensor<*>> =
        sensors.values.filter { it.kind == kind }

    fun size(): Int = sensors.size
}
