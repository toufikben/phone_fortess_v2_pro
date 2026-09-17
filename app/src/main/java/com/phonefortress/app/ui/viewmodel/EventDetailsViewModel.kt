package com.phonefortress.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonefortress.app.data.repository.EventRepository
import com.phonefortress.app.domain.model.SecurityEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventDetailsState(
    val loading: Boolean = true,
    val event: SecurityEvent? = null,
    val missing: Boolean = false
)

@HiltViewModel
class EventDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: EventRepository
) : ViewModel() {
    private val eventId: String? = savedStateHandle["eventId"]
    private val _state = MutableStateFlow(EventDetailsState())
    val state: StateFlow<EventDetailsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val event = eventId?.let { repository.getById(it) }
            _state.value = EventDetailsState(loading = false, event = event, missing = event == null)
        }
    }
}
