package com.phonefortress.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonefortress.app.domain.model.SafeZone
import com.phonefortress.app.domain.model.ZoneType
import com.phonefortress.app.geofence.ZoneStateHolder
import com.phonefortress.app.geofence.ZoneSyncUseCase
import com.phonefortress.app.platform.location.LocationProvider
import com.phonefortress.app.data.repository.SafeZoneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class GeofenceState(
    val zones: List<SafeZone> = emptyList(),
    val currentZoneId: String? = null,
    val showAddDialog: Boolean = false,
    val dialogLocation: Pair<Double, Double>? = null,
    val message: String? = null
)

@HiltViewModel
class GeofenceViewModel @Inject constructor(
    private val repository: SafeZoneRepository,
    private val syncUseCase: ZoneSyncUseCase,
    private val locationProvider: LocationProvider,
    private val zoneState: ZoneStateHolder
) : ViewModel() {

    private val _state = MutableStateFlow(GeofenceState())
    val state: StateFlow<GeofenceState> = _state.asStateFlow()

    init {
        observeZones()
        observeCurrentZone()
    }

    private fun observeZones() {
        viewModelScope.launch {
            repository.observeAll().collect { zones ->
                _state.update { it.copy(zones = zones) }
            }
        }
    }

    private fun observeCurrentZone() {
        viewModelScope.launch {
            zoneState.currentZone.collect { zone ->
                _state.update { it.copy(currentZoneId = zone?.id) }
            }
        }
    }

    fun showAddDialog() {
        _state.update { it.copy(showAddDialog = true, dialogLocation = null) }
    }

    fun dismissAddDialog() {
        _state.update { it.copy(showAddDialog = false, dialogLocation = null) }
    }

    fun getCurrentLocationForDialog() {
        viewModelScope.launch {
            val loc = locationProvider.getCurrentLocation()
            if (loc != null) {
                _state.update { it.copy(dialogLocation = loc.latitude to loc.longitude) }
            } else {
                _state.update { it.copy(message = "فشل الحصول على الموقع") }
            }
        }
    }

    fun addZone(name: String, lat: Double, lng: Double, radius: Float, type: ZoneType) {
        viewModelScope.launch {
            val zone = SafeZone(
                id = UUID.randomUUID().toString(),
                name = name,
                latitude = lat,
                longitude = lng,
                radiusMeters = radius,
                type = type
            )
            val ok = syncUseCase.addZone(zone)
            _state.update {
                it.copy(
                    showAddDialog = false,
                    dialogLocation = null,
                    message = if (ok) "✓ أُضيفت المنطقة" else "⚠ أُضيفت محلياً فقط"
                )
            }
        }
    }

    fun deleteZone(id: String) {
        viewModelScope.launch {
            syncUseCase.removeZone(id)
            _state.update { it.copy(message = "تم الحذف") }
        }
    }

    fun toggleZone(id: String, enabled: Boolean) {
        viewModelScope.launch {
            val zone = repository.getById(id) ?: return@launch
            repository.save(zone.copy(enabled = enabled))
            syncUseCase()
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}
