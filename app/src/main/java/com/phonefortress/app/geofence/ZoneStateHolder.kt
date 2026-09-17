package com.phonefortress.app.geofence

import com.phonefortress.app.data.prefs.SecurityPrefs
import com.phonefortress.app.data.repository.SafeZoneRepository
import com.phonefortress.app.domain.model.SafeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZoneStateHolder @Inject constructor(
    private val securityPrefs: SecurityPrefs,
    private val safeZoneRepository: SafeZoneRepository
) {
    private val _currentZone = MutableStateFlow<SafeZone?>(null)
    val currentZone: StateFlow<SafeZone?> = _currentZone.asStateFlow()

    private val _isInSafeZone = MutableStateFlow<Boolean?>(null)
    val isInSafeZone: StateFlow<Boolean?> = _isInSafeZone.asStateFlow()

    suspend fun restore() {
        val persistedId = securityPrefs.currentZoneId.first()
        _currentZone.value = persistedId?.let { safeZoneRepository.getById(it) }
        _isInSafeZone.value = securityPrefs.inSafeZone.first()
    }

    suspend fun setCurrentZone(zone: SafeZone?) {
        _currentZone.value = zone
        securityPrefs.setCurrentZoneId(zone?.id)
    }

    fun getCurrentZone(): SafeZone? = _currentZone.value

    suspend fun setInSafeZone(value: Boolean?) {
        _isInSafeZone.value = value
        securityPrefs.setInSafeZone(value)
    }

    fun effectiveThreshold(defaultThreshold: Int): Int =
        _currentZone.value?.effectiveThreshold() ?: defaultThreshold
}
