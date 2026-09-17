package com.phonefortress.app.platform.location

import android.location.Location
import android.os.SystemClock
import com.phonefortress.app.util.Constants

object LocationFreshness {
    fun isFresh(
        location: Location,
        nowWallClockMs: Long = System.currentTimeMillis(),
        nowElapsedRealtimeNanos: Long = SystemClock.elapsedRealtimeNanos()
    ): Boolean {
        val wallClockAge = nowWallClockMs - location.time
        val elapsedAge = if (location.elapsedRealtimeNanos > 0) {
            nowElapsedRealtimeNanos - location.elapsedRealtimeNanos
        } else Long.MAX_VALUE
        return isFresh(wallClockAge, elapsedAge, location.accuracy)
    }

    fun isFresh(wallClockAgeMs: Long, elapsedAgeNanos: Long, accuracyMeters: Float): Boolean =
        wallClockAgeMs in 0..Constants.MAX_LOCATION_AGE_MS &&
            elapsedAgeNanos in 0..Constants.MAX_LOCATION_AGE_MS * 1_000_000L &&
            accuracyMeters in 0f..Constants.MAX_LOCATION_ACCURACY_METERS
}
