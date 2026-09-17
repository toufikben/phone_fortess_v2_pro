package com.phonefortress.app.geofence

import com.phonefortress.app.domain.model.SafeZone
import com.phonefortress.app.util.Logger
import kotlin.math.*

/**
 * مُطابق المناطق — يحدد إذا كانت إحداثيات داخل منطقة.
 * يستخدم Haversine لتحديد المسافة الفعلية.
 */
object ZoneMatcher {

    private const val EARTH_RADIUS_M = 6_371_000.0

    /**
     * يجد أول منطقة تحتوي النقطة المعطاة.
     * إن لم يجد، يُرجع null (يعني محايد).
     */
    fun findMatchingZone(
        latitude: Double,
        longitude: Double,
        zones: List<SafeZone>
    ): SafeZone? {
        val matches = zones
            .filter { it.enabled }
            .map { it to distanceMeters(latitude, longitude, it.latitude, it.longitude) }
            .filter { (zone, distance) -> distance <= zone.radiusMeters }
            .sortedBy { (zone, _) -> zone.radiusMeters }  // الأصغر أولاً

        val match = matches.firstOrNull()?.first
        if (match != null) {
            Logger.d("Zone matched: ${match.name} (${match.type})")
        }
        return match
    }

    /**
     * مسافة Haversine بالأمتار.
     */
    fun distanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_M * c
    }
}
