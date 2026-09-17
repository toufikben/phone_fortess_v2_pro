package com.phonefortress.app.ui.viewmodel

import android.Manifest
import android.os.Build

object ProtectionPermissions {
    fun required(capturePhoto: Boolean, captureAudio: Boolean, captureLocation: Boolean): Array<String> = buildList {
        if (capturePhoto) add(Manifest.permission.CAMERA)
        if (captureAudio) add(Manifest.permission.RECORD_AUDIO)
        if (captureLocation) add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()
}
