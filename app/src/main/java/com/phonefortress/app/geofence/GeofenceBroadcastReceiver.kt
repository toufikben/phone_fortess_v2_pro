package com.phonefortress.app.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.phonefortress.app.data.repository.SafeZoneRepository
import com.phonefortress.app.util.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * مستقبل Geofence — يعالج أحداث الدخول/الخروج.
 * يُحدّث الحالة الحالية (داخل/خارج المناطق).
 */
@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var safeZoneRepository: SafeZoneRepository
    @Inject lateinit var zoneState: ZoneStateHolder

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return

        if (event.hasError()) {
            Logger.e(null, "Geofence error: ${event.errorCode}")
            return
        }

        val transitionType = event.geofenceTransition
        val triggeringGeofences = event.triggeringGeofences ?: return

        scope.launch {
            try {
                triggeringGeofences.forEach { geofence ->
                    val zoneId = geofence.requestId.removePrefix("pf_zone_")
                    val zone = safeZoneRepository.getById(zoneId) ?: return@forEach

                    when (transitionType) {
                        Geofence.GEOFENCE_TRANSITION_ENTER -> {
                            Logger.i("Entered zone: ${zone.name} (${zone.type})")
                            zoneState.setCurrentZone(zone)
                            zoneState.setInSafeZone(zone.type == com.phonefortress.app.domain.model.ZoneType.SAFE)
                        }
                        Geofence.GEOFENCE_TRANSITION_EXIT -> {
                            Logger.i("Exited zone: ${zone.name}")
                            if (zoneState.getCurrentZone()?.id == zone.id) {
                                zoneState.setCurrentZone(null)
                                zoneState.setInSafeZone(null)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.e(e, "Geofence event handling failed")
            }
        }
    }
}
