package com.nexus.feature.memory.domain.repository

import com.nexus.feature.memory.domain.model.EpisodicEvent
import kotlinx.coroutines.flow.Flow

/**
 * Repository for episodic memory — append-only event storage with time-based cleanup.
 */
interface EpisodicMemoryRepository {

    /** Persist a single event. */
    suspend fun record(event: EpisodicEvent)

    /** Observe the most recent [limit] events, ordered by collection time descending. */
    fun observeRecent(limit: Int = 100): Flow<List<EpisodicEvent>>

    /** Query events by source package. */
    suspend fun queryByPackage(packageName: String, limit: Int = 100): List<EpisodicEvent>

    /** Delete events older than [cutoffMs]. */
    suspend fun cleanup(cutoffMs: Long): Int

    /** Observe count of events whose collection time falls in [startMs, endMs). */
    fun observeCountInRange(startMs: Long, endMs: Long): Flow<Int>
}
