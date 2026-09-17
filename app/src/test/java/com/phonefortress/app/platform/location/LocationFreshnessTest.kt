package com.phonefortress.app.platform.location

import com.google.common.truth.Truth.assertThat
import com.phonefortress.app.util.Constants
import org.junit.jupiter.api.Test

class LocationFreshnessTest {
    @Test fun `fresh accurate location is accepted`() {
        assertThat(LocationFreshness.isFresh(50_000L, 50_000_000_000L, 25f)).isTrue()
    }

    @Test fun `stale location is rejected`() {
        assertThat(LocationFreshness.isFresh(Constants.MAX_LOCATION_AGE_MS + 1, 61_000_000_000L, 25f)).isFalse()
    }

    @Test fun `poor accuracy and future timestamps are rejected`() {
        assertThat(LocationFreshness.isFresh(50_000L, 50_000_000_000L, Constants.MAX_LOCATION_ACCURACY_METERS + 1f)).isFalse()
        assertThat(LocationFreshness.isFresh(-1L, -1L, 25f)).isFalse()
    }
}
