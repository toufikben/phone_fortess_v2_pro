package com.phonefortress.app.ui.viewmodel

import android.Manifest
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ProtectionPermissionsTest {
    @Test fun `photo only requests camera`() {
        assertThat(ProtectionPermissions.required(true, false, false).toList()).contains(Manifest.permission.CAMERA)
    }

    @Test fun `audio and location request their permissions`() {
        val permissions = ProtectionPermissions.required(false, true, true).toList()
        assertThat(permissions).contains(Manifest.permission.RECORD_AUDIO)
        assertThat(permissions).contains(Manifest.permission.ACCESS_FINE_LOCATION)
        assertThat(permissions).doesNotContain(Manifest.permission.CAMERA)
    }
}
