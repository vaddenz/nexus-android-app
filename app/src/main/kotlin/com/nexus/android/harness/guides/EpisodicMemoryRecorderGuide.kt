package com.nexus.android.harness.guides

import com.nexus.android.harness.pipelines.ImMessageContext
import com.nexus.core.eventbus.subscribe
import com.nexus.feature.memory.domain.usecase.RecordMessageUseCase
import com.nexus.harness.guide.Guide
import com.nexus.harness.guide.GuideContext
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guide that listens for [ImMessageContext] events on the bus and writes them
 * into the local episodic memory database.
 *
 * This keeps the side-effect (DB write) out of the Pipeline layer and binds it
 * to a lifecycle-managed component.
 */
@Singleton
class EpisodicMemoryRecorderGuide @Inject constructor(
    private val recordMessage: RecordMessageUseCase,
) : Guide {

    override val id = "episodic-memory-recorder"
    override val name = "Episodic Memory Recorder"

    override suspend fun onRun(ctx: GuideContext) {
        ctx.eventBus.events
            .filterIsInstance<ImMessageContext>()
            .collect { msg ->
                ctx.scope.launch {
                    recordMessage(
                        packageName = msg.packageName,
                        senderName = msg.senderName,
                        content = msg.content,
                        confidence = msg.confidence,
                        isDesensitized = msg.isDesensitized,
                    )
                }
            }
    }
}
