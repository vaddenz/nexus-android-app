package com.nexus.feature.memory.data.repository

import com.nexus.feature.memory.data.local.EpisodicEventDao
import com.nexus.feature.memory.data.local.EpisodicEventEntity
import com.nexus.feature.memory.domain.model.EpisodicEvent
import com.nexus.feature.memory.domain.repository.EpisodicMemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpisodicMemoryRepositoryImpl @Inject constructor(
    private val dao: EpisodicEventDao,
) : EpisodicMemoryRepository {

    override suspend fun record(event: EpisodicEvent) {
        dao.insert(event.toEntity())
    }

    override fun observeRecent(limit: Int): Flow<List<EpisodicEvent>> =
        dao.observeRecent(limit).map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun queryByPackage(packageName: String, limit: Int): List<EpisodicEvent> =
        dao.queryByPackage(packageName, limit).map { it.toDomain() }

    override suspend fun cleanup(cutoffMs: Long): Int =
        dao.deleteOlderThan(cutoffMs)

    private fun EpisodicEvent.toEntity() = EpisodicEventEntity(
        id = id,
        eventType = eventType,
        sourcePackage = sourcePackage,
        senderName = senderName,
        contentText = contentText,
        occurredAt = occurredAt,
        collectedAt = collectedAt,
        confidence = confidence,
        isDesensitized = isDesensitized,
    )

    private fun EpisodicEventEntity.toDomain() = EpisodicEvent(
        id = id,
        eventType = eventType,
        sourcePackage = sourcePackage,
        senderName = senderName,
        contentText = contentText,
        occurredAt = occurredAt,
        collectedAt = collectedAt,
        confidence = confidence,
        isDesensitized = isDesensitized,
    )
}
