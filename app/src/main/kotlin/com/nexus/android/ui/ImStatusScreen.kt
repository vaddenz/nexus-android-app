package com.nexus.android.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.android.R
import com.nexus.feature.memory.domain.model.EpisodicEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImStatusScreen(viewModel: ImStatusViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshAccessibilityStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("IM 采集状态") })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            item { AccessibilityStatusCard(state.isAccessibilityEnabled) }

            if (!state.isAccessibilityEnabled) {
                item {
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.open_settings))
                    }
                }
            }

            item { TodayCountCard(state.todayCount) }
            item { Spacer(Modifier.height(8.dp)) }
            item { Text("最近采集", style = MaterialTheme.typography.titleMedium) }

            if (state.recentMessages.isEmpty()) {
                item {
                    Text(
                        "暂无采集记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.recentMessages, key = { it.id }) { event ->
                    MessageCard(event)
                }
            }
        }
    }
}

@Composable
private fun AccessibilityStatusCard(enabled: Boolean) {
    val containerColor = if (enabled) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
    val contentColor = if (enabled) Color(0xFF2E7D32) else Color(0xFFEF6C00)
    val icon = if (enabled) Icons.Default.CheckCircle else Icons.Default.Warning
    val label = if (enabled) {
        stringResource(R.string.accessibility_status_enabled)
    } else {
        stringResource(R.string.accessibility_status_disabled)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun TodayCountCard(count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.today_collected),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                stringResource(R.string.messages_count, count),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Composable
private fun MessageCard(event: EpisodicEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    event.senderName.takeIf { it.isNotBlank() } ?: "未知发送者",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    event.sourcePackage.substringAfterLast("."),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                event.contentText,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (event.isDesensitized) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "已脱敏",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
