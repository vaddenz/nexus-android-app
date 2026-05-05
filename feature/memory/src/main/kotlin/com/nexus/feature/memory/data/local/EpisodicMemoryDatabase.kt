package com.nexus.feature.memory.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        EpisodicEventEntity::class,
        EpisodicEventFts::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class EpisodicMemoryDatabase : RoomDatabase() {
    abstract fun eventDao(): EpisodicEventDao
}
