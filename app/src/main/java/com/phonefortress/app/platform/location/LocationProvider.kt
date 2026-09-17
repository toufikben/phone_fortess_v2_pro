package com.phonefortress.app.platform.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.phonefortress.app.util.Constants
import com.phonefortress.app.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * مزود الموقع — يحصل على آخر موقع معروف أو يطلب تحديثاً واحداً.
 */
@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    data class LocationResult(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float
    )

    /**
     * يُحاول الحصول على الموقع بدقة عالية.
     */
    suspend fun getCurrentLocation(): LocationResult? = withContext(Dispatchers.IO) {
        if (!hasPermission()) {
            Logger.w("Location permission not granted")
            return@withContext null
        }

        try {
            // 1. حاول آخر موقع معروف
            val last = tryGetLastLocation()
            if (last != null && isRecent(last)) {
                return@withContext last
            }

            // 2. اطلب تحديثاً واحداً
            requestSingleUpdate() ?: last
        } catch (e: SecurityException) {
            Logger.e(e, "Location security exception")
            null
        } catch (e: Exception) {
            Logger.e(e, "Location provider failed")
            null
        }
    }

    private suspend fun tryGetLastLocation(): LocationResult? =
        suspendCancellableCoroutine { cont ->
            try {
                client.lastLocation
                    .addOnSuccessListener { loc ->
                        if (cont.isActive) cont.resume(loc?.toResult())
                    }
                    .addOnFailureListener { e ->
                        Logger.w("Last location failed: ${e.message}")
                        if (cont.isActive) cont.resume(null)
                    }
            } catch (e: SecurityException) {
                if (cont.isActive) cont.resume(null)
            }
        }

    private suspend fun requestSingleUpdate(): LocationResult? =
        suspendCancellableCoroutine { cont ->
            try {
                val request = com.google.android.gms.location.CurrentLocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    Constants.LOCATION_TIMEOUT_MS
                ).build()

                client.getCurrentLocation(request, null)
                    .addOnSuccessListener { loc ->
                        if (cont.isActive) cont.resume(loc?.toResult())
                    }
                    .addOnFailureListener { e ->
                        Logger.w("Current location failed: ${e.message}")
                        if (cont.isActive) cont.resume(null)
                    }
            } catch (e: SecurityException) {
                if (cont.isActive) cont.resume(null)
            }
        }

    private fun Location.toResult() = LocationResult(latitude, longitude, accuracy)

    private fun isRecent(loc: Location): Boolean =
        System.currentTimeMillis() - loc.time < 60_000L

    private fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
}
