package com.nexus.harness.pipeline

import java.util.concurrent.ConcurrentHashMap

class PipelineRegistry {

    private val pipelines = ConcurrentHashMap<String, Pipeline<*>>()

    fun register(pipeline: Pipeline<*>): Boolean =
        pipelines.putIfAbsent(pipeline.id, pipeline) == null

    fun unregister(pipelineId: String): Pipeline<*>? = pipelines.remove(pipelineId)

    fun get(pipelineId: String): Pipeline<*>? = pipelines[pipelineId]

    fun all(): List<Pipeline<*>> = pipelines.values.toList()

    fun size(): Int = pipelines.size
}
