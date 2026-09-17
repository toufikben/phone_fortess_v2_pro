package com.phonefortress.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phonefortress.app.R
import com.phonefortress.app.domain.model.EventOperation
import com.phonefortress.app.domain.model.SecurityEventStatus
import com.phonefortress.app.ui.viewmodel.EventDetailsViewModel
import java.text.DateFormat
import java.util.Date
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(
    onBack: () -> Unit,
    viewModel: EventDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.event_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.event_detail_back))
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.missing -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(stringResource(R.string.event_detail_missing)) }
            else -> state.event?.let { event ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.event_detail_status, statusLabel(event.status)), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.event_detail_time, DateFormat.getDateTimeInstance().format(Date(event.timestamp))))
                                Text(stringResource(R.string.event_detail_operation, operationLabel(event.operation)))
                                Text(stringResource(R.string.threat_score_title) + ": ${event.threatScore}/100")
                                event.lastTransitionReason?.takeIf { it.isNotBlank() }?.let {
                                    Text(stringResource(R.string.event_detail_result, it))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun statusLabel(status: SecurityEventStatus): String = stringResource(
    when (status) {
        SecurityEventStatus.PENDING -> R.string.event_status_pending
        SecurityEventStatus.DEFERRED -> R.string.event_status_deferred
        SecurityEventStatus.IN_PROGRESS -> R.string.event_status_in_progress
        SecurityEventStatus.CAPTURED -> R.string.event_status_captured
        SecurityEventStatus.SEND_PENDING -> R.string.event_status_send_pending
        SecurityEventStatus.SENT -> R.string.event_status_sent
        SecurityEventStatus.FAILED_RETRYABLE -> R.string.event_status_failed_retryable
        SecurityEventStatus.FAILED_FINAL -> R.string.event_status_failed_final
        SecurityEventStatus.CANCELLED -> R.string.event_status_cancelled
    }
)

@Composable
private fun operationLabel(operation: EventOperation): String = stringResource(
    if (operation == EventOperation.CAPTURE) R.string.event_operation_capture else R.string.event_operation_send
)
