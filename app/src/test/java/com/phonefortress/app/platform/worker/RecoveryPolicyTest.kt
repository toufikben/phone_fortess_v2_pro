package com.phonefortress.app.platform.worker

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class RecoveryPolicyTest {
    @Test
    fun `in-progress event is stale only after persisted timeout`() {
        assertThat(EventRecoveryWorker.isStale(100L, 120_000L)).isFalse()
        assertThat(EventRecoveryWorker.isStale(100L, 120_100L)).isTrue()
    }
}
