package com.nexus.android.ui

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.android.service.ImAccessibilityService
import com.nexus.feature.memory.domain.model.EpisodicEvent
import com.nexus.feature.memory.domain.usecase.QueryRecentMessagesUseCase
import com.nexus.feature.memory.domain.usecase.QueryTodayMessageCountUseCase
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
    val selectedMessage: EpisodicEvent? = null,
)

@HiltViewModel
class ImStatusViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    queryRecent: QueryRecentMessagesUseCase,
    queryTodayCount: QueryTodayMessageCountUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ImStatusUiState())
    val state: StateFlow<ImStatusUiState> = _state.asStateFlow()

    init {
        refreshAccessibilityStatus()

        queryRecent(limit = 50)
            .onEach { events ->
                _state.update { it.copy(recentMessages = events) }
            }
            .launchIn(viewModelScope)

        queryTodayCount()
            .onEach { count ->
                _state.update { it.copy(todayCount = count) }
            }
            .launchIn(viewModelScope)
    }

    fun refreshAccessibilityStatus() {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: ""
        val componentName = ComponentName(context, ImAccessibilityService::class.java).flattenToString()
        _state.update {
            it.copy(isAccessibilityEnabled = enabledServices.contains(componentName))
        }
    }

    fun selectMessage(event: EpisodicEvent?) {
        _state.update { it.copy(selectedMessage = event) }
    }
}
