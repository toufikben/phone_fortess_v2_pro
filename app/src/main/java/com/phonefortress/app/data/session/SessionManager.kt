package com.phonefortress.app.data.session

import com.phonefortress.app.data.prefs.AutoLockPrefs
import com.phonefortress.app.data.prefs.PinPrefs
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val pinPrefs: PinPrefs,
    private val autoLockPrefs: AutoLockPrefs
) {
    suspend fun shouldRequireUnlock(): Boolean {
        if (!pinPrefs.isPinEnabled.first()) return false
        val timeoutSeconds = autoLockPrefs.timeoutSeconds.first()
        if (timeoutSeconds <= 0) return false
        val lastUnlock = pinPrefs.lastUnlock.first()
        return lastUnlock <= 0L || System.currentTimeMillis() - lastUnlock >= timeoutSeconds * 1000L
    }

    suspend fun markUnlocked() = pinPrefs.markUnlocked()
}
