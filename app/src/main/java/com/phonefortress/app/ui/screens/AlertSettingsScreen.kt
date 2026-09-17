package com.phonefortress.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phonefortress.app.ui.viewmodel.AlertSettingsViewModel
import androidx.compose.ui.res.stringResource
import com.phonefortress.app.R

/**
 * شاشة إدارة قنوات التنبيه.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertSettingsScreen(
    onBack: () -> Unit,
    viewModel: AlertSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.alert_channels_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "اختر قناة أو أكثر لإرسال التنبيهات. كل القنوات ترسل مباشرة من الجهاز — لا خدمات سحابية.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(state.channels, key = { it.id }) { channel ->
                ChannelRow(
                    channel = channel,
                    onToggle = { viewModel.toggleChannel(channel.id, it) },
                    onTest = { viewModel.testChannel(channel.id) }
                )
            }

            if (state.lastTestResult != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.lastTestSuccess)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = state.lastTestResult!!,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(
    channel: ChannelUiModel,
    onToggle: (Boolean) -> Unit,
    onTest: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (channel.configured) {
                        Text(
                            text = "✓ مهيأة",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (channel.requiresConfig) {
                        Text(
                            text = "⚠ تحتاج إعداد",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Switch(
                    checked = channel.enabled,
                    onCheckedChange = onToggle
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (channel.configured) {
                    TextButton(onClick = onTest) { Text(stringResource(R.string.common_ok)) }
                }
            }
        }
    }
}

data class ChannelUiModel(
    val id: String,
    val displayName: String,
    val enabled: Boolean,
    val configured: Boolean,
    val requiresConfig: Boolean
)
