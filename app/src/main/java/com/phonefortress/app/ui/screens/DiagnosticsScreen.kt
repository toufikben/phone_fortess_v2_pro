package com.phonefortress.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phonefortress.app.ui.theme.LocalThemeTokens
import com.phonefortress.app.ui.viewmodel.DiagnosticsViewModel
import androidx.compose.ui.res.stringResource
import com.phonefortress.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onBack: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val workers by viewModel.workers.collectAsStateWithLifecycle()
    val tokens = LocalThemeTokens.current
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.diagnostics_title)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back)) } },
            actions = { TextButton(onClick = viewModel::refresh) { Text(stringResource(R.string.common_retry)) } }
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { InfoCard(stringResource(R.string.diagnostics_workers), workers.count { it.state == WorkInfoState.RUNNING }.toString()) }
            items(workers, key = { it.id }) { worker ->
                Card(colors = CardDefaults.cardColors(containerColor = tokens.surface)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(worker.tag, style = MaterialTheme.typography.titleSmall, color = tokens.textPrimary)
                        Text(stringResource(R.string.diagnostics_status, worker.state), style = MaterialTheme.typography.bodySmall, color = tokens.textSecondary)
                        Text(stringResource(R.string.diagnostics_attempts, worker.runAttemptCount), style = MaterialTheme.typography.bodySmall, color = tokens.textSecondary)
                    }
                }
            }
        }
    }
}

private object WorkInfoState { const val RUNNING = "RUNNING" }

@Composable
private fun InfoCard(title: String, value: String) {
    val tokens = LocalThemeTokens.current
    Card(colors = CardDefaults.cardColors(containerColor = tokens.primary.copy(alpha = 0.15f))) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = tokens.textSecondary, style = MaterialTheme.typography.labelMedium)
            Text(value, color = tokens.primary, style = MaterialTheme.typography.headlineMedium)
        }
    }
}
