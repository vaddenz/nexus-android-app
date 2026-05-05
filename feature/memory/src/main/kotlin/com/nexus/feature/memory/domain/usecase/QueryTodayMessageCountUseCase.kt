package com.nexus.feature.memory.domain.usecase

import com.nexus.feature.memory.domain.repository.EpisodicMemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.ZoneId
import javax.inject.Inject

/**
 * Observes the count of messages collected today in the device's default timezone.
 */
class QueryTodayMessageCountUseCase @Inject constructor(
    private val repository: EpisodicMemoryRepository,
) {
    operator fun invoke(zone: ZoneId = ZoneId.systemDefault()): Flow<Int> {
        val now = java.time.ZonedDateTime.now(zone)
        val startOfDay = now.toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        val startOfNextDay = now.toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return repository.observeCountInRange(startOfDay, startOfNextDay)
    }
}
