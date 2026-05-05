package com.nexus.feature.memory.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "episodic_events")
data class EpisodicEventEntity(
    @PrimaryKey val id: String,
    val eventType: String,
    val sourcePackage: String,
    val senderName: String,
    val contentText: String,
    val occurredAt: Long,
    val collectedAt: Long,
    val confidence: Float,
    val isDesensitized: Boolean,
)
