package com.nexus.feature.memory.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodicEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EpisodicEventEntity)

    @Query("SELECT * FROM episodic_events ORDER BY collectedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<EpisodicEventEntity>>

    @Query("SELECT * FROM episodic_events WHERE sourcePackage = :pkg ORDER BY collectedAt DESC LIMIT :limit")
    suspend fun queryByPackage(pkg: String, limit: Int = 100): List<EpisodicEventEntity>

    @Query("DELETE FROM episodic_events WHERE collectedAt < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long): Int

    @Query("SELECT COUNT(*) FROM episodic_events")
    suspend fun count(): Int
}
