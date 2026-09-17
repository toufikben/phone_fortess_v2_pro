package com.phonefortress.app.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.phonefortress.app.domain.model.SafeZone
import com.phonefortress.app.domain.model.ZoneType
import com.phonefortress.app.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مدير Geofence — يسجّل المناطق مع Google Play Services.
 * عند الدخول/الخروج، يُطلق BroadcastReceiver.
 */
@Singleton
class GeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val client: GeofencingClient by lazy {
        LocationServices.getGeofencingClient(context)
    }

    companion object {
        private const val GEOFENCE_REQUEST_ID = "pf_zone_"
        private const val ACTION_GEOFENCE = "com.phonefortress.app.GEOFENCE_EVENT"
    }

    /**
     * يُسجّل كل المناطق المفعّلة.
     * يمسح السابق أولاً.
     */
    @SuppressLint("MissingPermission")
    suspend fun registerZones(zones: List<SafeZone>): Boolean = withContext(Dispatchers.IO) {
        try {
            unregisterAll()

            val active = zones.filter { it.enabled }
            if (active.isEmpty()) {
                Logger.i("No zones to register")
                return@withContext true
            }

            val geofences = active.map { zone ->
                Geofence.Builder()
                    .setRequestId("$GEOFENCE_REQUEST_ID${zone.id}")
                    .setCircularRegion(
                        zone.latitude,
                        zone.longitude,
                        zone.radiusMeters.coerceAtLeast(100f)
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(
                        Geofence.GEOFENCE_TRANSITION_ENTER or
                                Geofence.GEOFENCE_TRANSITION_EXIT
                    )
                    .build()
            }

            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofences)
                .build()

            client.addGeofences(request, getPendingIntent()).await()
            Logger.i("Registered ${active.size} geofences")
            true
        } catch (e: SecurityException) {
            Logger.e(e, "Geofence permission denied")
            false
        } catch (e: Exception) {
            Logger.e(e, "Geofence register failed")
            false
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun unregisterAll(): Boolean = withContext(Dispatchers.IO) {
        try {
            client.removeGeofences(getPendingIntent()).await()
            true
        } catch (e: Exception) {
            Logger.w("Unregister geofences: ${e.message}")
            false
        }
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = ACTION_GEOFENCE
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            flags
        )
    }
}
