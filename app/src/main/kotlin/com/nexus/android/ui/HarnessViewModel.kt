package com.nexus.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.android.harness.pipelines.ChargingTrendContext
import com.nexus.android.harness.pipelines.StepActivityContext
import com.nexus.android.harness.sensors.BatteryReading
import com.nexus.android.harness.sensors.NetworkReading
import com.nexus.android.harness.sensors.StepEvent
import com.nexus.core.eventbus.EventBus
import com.nexus.core.eventbus.subscribe
import com.nexus.harness.sensor.SensorObservation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Aggregates harness output into a single [UiState] for [HarnessScreen].
 *
 * State updates happen on [viewModelScope]. The Compose layer collects via
 * [collectAsStateWithLifecycle], guaranteeing the subscription is paused while
 * the UI is not started — this is the lifecycle-safety contract for the UI.
 */
@HiltViewModel
class HarnessViewModel @Inject constructor(
    eventBus: EventBus,
) : ViewModel() {

    data class UiState(
        val battery: BatteryReading? = null,
        val network: NetworkReading? = null,
        val stepCount: Int = 0,
        val chargingTrend: ChargingTrendContext? = null,
        val stepActivity: StepActivityContext? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            eventBus.subscribe<SensorObservation>().collect { obs ->
                when (obs.sensorId) {
                    "battery" -> (obs.value as? BatteryReading)?.let { reading ->
                        _state.update { it.copy(battery = reading) }
                    }
                    "network" -> (obs.value as? NetworkReading)?.let { reading ->
                        _state.update { it.copy(network = reading) }
                    }
                    "step-cadence" -> if (obs.value is StepEvent) {
                        _state.update { it.copy(stepCount = it.stepCount + 1) }
                    }
                }
            }
        }
        viewModelScope.launch {
            eventBus.subscribe<ChargingTrendContext>().collect { ctx ->
                _state.update { it.copy(chargingTrend = ctx) }
            }
        }
        viewModelScope.launch {
            eventBus.subscribe<StepActivityContext>().collect { ctx ->
                _state.update { it.copy(stepActivity = ctx) }
            }
        }
    }
}
