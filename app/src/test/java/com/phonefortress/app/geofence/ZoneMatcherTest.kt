package com.phonefortress.app.geofence

import com.google.common.truth.Truth.assertThat
import com.phonefortress.app.domain.model.SafeZone
import com.phonefortress.app.domain.model.ZoneType
import org.junit.jupiter.api.Test

class ZoneMatcherTest {
    private val lat = 24.7136
    private val lng = 46.6753
    @Test fun `same point distance is zero`() { assertThat(ZoneMatcher.distanceMeters(lat, lng, lat, lng)).isWithin(0.01).of(0.0) }
    @Test fun `nearby point distance is reasonable`() { assertThat(ZoneMatcher.distanceMeters(lat, lng, lat + .01, lng)).isWithin(50.0).of(1113.0) }
    @Test fun `finds matching zone inside radius`() {
        val z = SafeZone("home", "Home", lat, lng, 200f, ZoneType.SAFE)
        assertThat(ZoneMatcher.findMatchingZone(lat + .001, lng + .001, listOf(z))?.id).isEqualTo("home")
    }
    @Test fun `returns null outside zones`() {
        val z = SafeZone("home", "Home", lat, lng, 100f, ZoneType.SAFE)
        assertThat(ZoneMatcher.findMatchingZone(lat + 1, lng + 1, listOf(z))).isNull()
    }
    @Test fun `ignores disabled zones`() {
        val z = SafeZone("off", "Off", lat, lng, 500f, ZoneType.SAFE, enabled = false)
        assertThat(ZoneMatcher.findMatchingZone(lat, lng, listOf(z))).isNull()
    }
    @Test fun `smallest overlapping zone wins`() {
        val big = SafeZone("big", "Big", lat, lng, 1000f, ZoneType.NEUTRAL)
        val small = SafeZone("small", "Small", lat, lng, 100f, ZoneType.DANGER)
        assertThat(ZoneMatcher.findMatchingZone(lat, lng, listOf(big, small))?.id).isEqualTo("small")
    }
}
