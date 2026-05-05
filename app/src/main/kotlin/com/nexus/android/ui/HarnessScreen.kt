package com.nexus.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HarnessScreen(viewModel: HarnessViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.harness_overview)) })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionHeader(stringResource(R.string.sensors_label)) }
            item { BatteryCard(state.battery) }
            item { NetworkCard(state.network) }
            item { StepCountCard(state.stepCount) }
            item { Spacer(Modifier.height(8.dp)) }
            item { SectionHeader(stringResource(R.string.pipelines_label)) }
            item { ChargingTrendCard(state.chargingTrend) }
            item { StepActivityCard(state.stepActivity) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun BatteryCard(reading: com.nexus.android.harness.sensors.BatteryReading?) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Battery", style = MaterialTheme.typography.titleSmall)
            if (reading == null) {
                Text("waiting…", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("${reading.levelPercent}% — ${if (reading.charging) "charging" else "on battery"}",
                    style = MaterialTheme.typography.bodyMedium)
                Text("plug: ${reading.plugType}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun NetworkCard(reading: com.nexus.android.harness.sensors.NetworkReading?) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Network", style = MaterialTheme.typography.titleSmall)
            if (reading == null) {
                Text("waiting…", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(if (reading.online) "online (${reading.transport})" else "offline",
                    style = MaterialTheme.typography.bodyMedium)
                Text("unmetered: ${reading.unmetered}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StepCountCard(count: Int) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Steps (since launch)", style = MaterialTheme.typography.titleSmall)
            Text("$count", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun ChargingTrendCard(ctx: com.nexus.android.harness.pipelines.ChargingTrendContext?) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Charging Trend", style = MaterialTheme.typography.titleSmall)
            if (ctx == null) {
                Text("collecting samples…", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("${ctx.trend} (Δ ${ctx.deltaPercent}%)",
                    style = MaterialTheme.typography.bodyMedium)
                Text("window: ${ctx.windowMs / 1000}s", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StepActivityCard(ctx: com.nexus.android.harness.pipelines.StepActivityContext?) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Step Activity", style = MaterialTheme.typography.titleSmall)
            if (ctx == null) {
                Text("walk to populate…", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("${ctx.level} — ${ctx.cadenceStepsPerMin.toInt()} steps/min",
                    style = MaterialTheme.typography.bodyMedium)
                Text("${ctx.stepsInWindow} steps in last window",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
