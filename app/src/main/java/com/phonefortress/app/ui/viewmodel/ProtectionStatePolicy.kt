package com.phonefortress.app.ui.viewmodel

object ProtectionStatePolicy {
    fun isActive(requested: Boolean, deviceAdminActive: Boolean): Boolean = requested && deviceAdminActive
    fun canEnable(deviceAdminActive: Boolean): Boolean = deviceAdminActive
}
