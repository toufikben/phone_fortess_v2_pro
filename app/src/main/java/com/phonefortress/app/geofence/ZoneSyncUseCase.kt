package com.phonefortress.app.geofence

import com.phonefortress.app.data.repository.SafeZoneRepository
import com.phonefortress.app.domain.model.SafeZone
import com.phonefortress.app.util.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * حالة استخدام: مزامنة المناطق مع النظام.
 * تُستدعى عند بدء التطبيق، عند تعديل المناطق، أو بعد الإقلاع.
 */
@Singleton
class ZoneSyncUseCase @Inject constructor(
    private val repository: SafeZoneRepository,
    private val manager: GeofenceManager
) {
    suspend operator fun invoke(): Boolean {
        val zones = repository.getAllEnabled()
        Logger.i("Syncing ${zones.size} zones")
        return manager.registerZones(zones)
    }

    suspend fun addZone(zone: SafeZone): Boolean {
        repository.save(zone)
        return invoke()
    }

    suspend fun removeZone(id: String): Boolean {
        repository.delete(id)
        return invoke()
    }
}
