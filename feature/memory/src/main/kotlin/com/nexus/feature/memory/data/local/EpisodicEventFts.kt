package com.nexus.feature.memory.data.local

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = EpisodicEventEntity::class)
@Entity(tableName = "episodic_events_fts")
data class EpisodicEventFts(
    val contentText: String,
    val senderName: String,
)
