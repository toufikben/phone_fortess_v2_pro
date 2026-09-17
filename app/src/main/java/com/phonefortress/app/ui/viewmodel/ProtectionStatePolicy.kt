package com.phonefortress.app.ui.viewmodel

object ProtectionStatePolicy {
    fun isActive(
        requested: Boolean,
        deviceAdminActive: Boolean,
        requiredPermissionsGranted: Boolean
    ): Boolean = requested && deviceAdminActive && requiredPermissionsGranted

    fun canEnable(deviceAdminActive: Boolean, requiredPermissionsGranted: Boolean): Boolean =
        deviceAdminActive && requiredPermissionsGranted
}
