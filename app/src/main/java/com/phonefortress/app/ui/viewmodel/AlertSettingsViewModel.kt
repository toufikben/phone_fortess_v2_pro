package com.phonefortress.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonefortress.app.alerts.AlertDispatcher
import com.phonefortress.app.data.prefs.AlertPrefs
import com.phonefortress.app.domain.model.AlertResult
import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.domain.model.ThreatLevel
import com.phonefortress.app.ui.screens.ChannelUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertSettingsState(
    val channels: List<ChannelUiModel> = emptyList(),
    val loading: Boolean = true,
    val lastTestResult: String? = null,
    val lastTestSuccess: Boolean = false
)

@HiltViewModel
class AlertSettingsViewModel @Inject constructor(
    private val dispatcher: AlertDispatcher,
    private val prefs: AlertPrefs
) : ViewModel() {

    private val _state = MutableStateFlow(AlertSettingsState())
    val state: StateFlow<AlertSettingsState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val statuses = dispatcher.channelStatuses()
            val models = statuses.map {
                ChannelUiModel(
                    id = it.id,
                    displayName = it.displayName,
                    enabled = it.enabled,
                    configured = it.configured,
                    requiresConfig = it.id != "local"
                )
            }.sortedBy { it.id }
            _state.update { it.copy(channels = models, loading = false) }
        }
    }

    fun toggleChannel(id: String, enabled: Boolean) {
        viewModelScope.launch {
            when (id) {
                "telegram" -> prefs.setTelegramEnabled(enabled)
                "ntfy" -> prefs.setNtfyEnabled(enabled)
                "smtp" -> prefs.setSmtpEnabled(enabled)
                "webhook" -> prefs.setWebhookEnabled(enabled)
                "sms" -> prefs.setSmsEnabled(enabled)
                "local" -> prefs.setLocalEnabled(enabled)
            }
            refresh()
        }
    }

    fun openConfig(id: String) {
        // TODO: navigation to config screen per channel
    }

    fun testChannel(id: String) {
        viewModelScope.launch {
            val status = _state.value.channels.firstOrNull { it.id == id } ?: return@launch
            if (!status.configured) {
                _state.update {
                    it.copy(
                        lastTestResult = "❌ القناة غير مهيأة",
                        lastTestSuccess = false
                    )
                }
                return@launch
            }
            _state.update { it.copy(lastTestResult = null) }
            val success = runCatching {
                val testEvent = SecurityEvent(
                    id = "test-${System.currentTimeMillis()}",
                    timestamp = System.currentTimeMillis(),
                    failedAttempts = 3,
                    threshold = 3,
                    isTest = true,
                    threatScore = 50,
                    threatLevel = ThreatLevel.MEDIUM
                )
                dispatcher.dispatch(testEvent).any { it is AlertResult.Success }
            }.getOrDefault(false)
            _state.update {
                it.copy(
                    lastTestResult = if (success) "✓ نجح الإرسال" else "❌ فشل الإرسال",
                    lastTestSuccess = success
                )
            }
        }
    }
}
