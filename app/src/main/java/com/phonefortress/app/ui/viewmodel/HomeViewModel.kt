package com.phonefortress.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonefortress.app.alerts.AlertDispatcher
import com.phonefortress.app.data.prefs.SecurityPrefs
import com.phonefortress.app.data.repository.EventRepository
import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val isProtectionActive: Boolean = false,
    val uptimeText: String = "",
    val stats: HomeStats = HomeStats(),
    val lastEvent: SecurityEvent? = null,
    val lastEventTimeAgo: String = ""
)

data class HomeStats(
    val attempts: Int = 0,
    val locks: Int = 0,
    val photos: Int = 0,
    val channelsActive: Int = 0,
    val channelsTotal: Int = 6
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val securityPrefs: SecurityPrefs,
    private val eventRepository: EventRepository,
    private val dispatcher: AlertDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private var protectionStartedAt: Long = 0

    init {
        observeProtection()
        observeEvents()
        observeChannelStatus()
        startUptimeTicker()
    }

    private fun observeProtection() {
        viewModelScope.launch {
            securityPrefs.protectionEnabled.collect { active ->
                if (active && protectionStartedAt == 0L) {
                    protectionStartedAt = System.currentTimeMillis()
                } else if (!active) {
                    protectionStartedAt = 0
                }
                _state.update { it.copy(isProtectionActive = active) }
            }
        }
    }

    private fun observeEvents() {
        viewModelScope.launch {
            eventRepository.observeRecent(20).collect { events ->
                val last = events.firstOrNull()
                _state.update {
                    it.copy(
                        lastEvent = last,
                        lastEventTimeAgo = last?.let { event -> timeAgo(event.timestamp) } ?: "",
                        stats = it.stats.copy(
                            attempts = events.size,
                            photos = events.count { event -> event.photoPath != null }
                        )
                    )
                }
            }
        }
    }

    private fun observeChannelStatus() {
        viewModelScope.launch {
            try {
                val statuses = dispatcher.channelStatuses()
                val active = statuses.count { it.configured }
                _state.update {
                    it.copy(
                        stats = it.stats.copy(
                            channelsActive = active,
                            channelsTotal = statuses.size
                        )
                    )
                }
            } catch (e: Exception) {
                Logger.w("Channel status fetch failed")
            }
        }
    }

    private fun startUptimeTicker() {
        viewModelScope.launch {
            while (true) {
                if (protectionStartedAt > 0) {
                    val elapsed = System.currentTimeMillis() - protectionStartedAt
                    _state.update { it.copy(uptimeText = formatUptime(elapsed)) }
                } else {
                    _state.update { it.copy(uptimeText = "") }
                }
                delay(1000)
            }
        }
    }

    fun toggleProtection() {
        viewModelScope.launch {
            val current = securityPrefs.protectionEnabled.first()
            securityPrefs.setProtectionEnabled(!current)
        }
    }

    private fun formatUptime(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        val hours = ms / (1000 * 60 * 60)
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }

    private fun timeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000 -> "قبل لحظات"
            diff < 3_600_000 -> "قبل ${diff / 60_000} دقيقة"
            diff < 86_400_000 -> "قبل ${diff / 3_600_000} ساعة"
            else -> "قبل ${diff / 86_400_000} يوم"
        }
    }
}
