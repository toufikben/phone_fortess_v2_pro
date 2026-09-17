package com.phonefortress.app.ui.viewmodel

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ProtectionStatePolicyTest {
    @Test fun `inactive admin cannot enable protection`() {
        assertThat(ProtectionStatePolicy.isActive(true, false, true)).isFalse()
        assertThat(ProtectionStatePolicy.canEnable(false, true)).isFalse()
    }

    @Test fun `missing permissions cannot enable protection`() {
        assertThat(ProtectionStatePolicy.isActive(true, true, false)).isFalse()
        assertThat(ProtectionStatePolicy.canEnable(true, false)).isFalse()
    }

    @Test fun `admin and all permissions enable protection`() {
        assertThat(ProtectionStatePolicy.isActive(true, true, true)).isTrue()
        assertThat(ProtectionStatePolicy.canEnable(true, true)).isTrue()
    }

    @Test fun `activation cancellation cannot enable protection`() {
        assertThat(ProtectionStatePolicy.canEnable(false, false)).isFalse()
        assertThat(ProtectionStatePolicy.isActive(false, false, false)).isFalse()
    }
}
