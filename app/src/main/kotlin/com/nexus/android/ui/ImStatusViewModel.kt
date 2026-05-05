package com.nexus.android.ui

import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.feature.memory.domain.model.EpisodicEvent
import com.nexus.feature.memory.domain.usecase.QueryRecentMessagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@Immutable
data class ImStatusUiState(
    val isAccessibilityEnabled: Boolean = false,
    val todayCount: Int = 0,
    val recentMessages: List<EpisodicEvent> = emptyList(),
)

@HiltViewModel
class ImStatusViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    queryRecent: QueryRecentMessagesUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ImStatusUiState())
    val state: StateFlow<ImStatusUiState> = _state.asStateFlow()

    init {
        refreshAccessibilityStatus()

        queryRecent(limit = 10)
            .onEach { events ->
                val now = System.currentTimeMillis()
                val startOfDay = now - (now % 86_400_000L)
                val todayCount = events.count { it.collectedAt >= startOfDay }
                _state.update {
                    it.copy(
                        todayCount = todayCount,
                        recentMessages = events,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun refreshAccessibilityStatus() {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: ""
        val serviceName = "${context.packageName}/.service.ImAccessibilityService"
        _state.update {
            it.copy(isAccessibilityEnabled = enabledServices.contains(serviceName))
        }
    }
}
