package com.nexus.feature.memory.domain.usecase

import com.nexus.feature.memory.domain.model.EpisodicEvent
import com.nexus.feature.memory.domain.repository.EpisodicMemoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observes the most recent episodic events.
 */
class QueryRecentMessagesUseCase @Inject constructor(
    private val repository: EpisodicMemoryRepository,
) {
    operator fun invoke(limit: Int = 100): Flow<List<EpisodicEvent>> =
        repository.observeRecent(limit)
}
