package com.phonefortress.app.ui.viewmodel

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ProtectionStatePolicyTest {
    @Test fun `requested protection is inactive when Device Admin is inactive`() {
        assertThat(ProtectionStatePolicy.isActive(requested = true, deviceAdminActive = false)).isFalse()
        assertThat(ProtectionStatePolicy.canEnable(deviceAdminActive = false)).isFalse()
    }

    @Test fun `requested protection is active only after Device Admin activation`() {
        assertThat(ProtectionStatePolicy.isActive(requested = true, deviceAdminActive = true)).isTrue()
        assertThat(ProtectionStatePolicy.canEnable(deviceAdminActive = true)).isTrue()
    }

    @Test fun `activation cancellation cannot enable protection`() {
        assertThat(ProtectionStatePolicy.canEnable(deviceAdminActive = false)).isFalse()
        assertThat(ProtectionStatePolicy.isActive(requested = false, deviceAdminActive = false)).isFalse()
    }
}
