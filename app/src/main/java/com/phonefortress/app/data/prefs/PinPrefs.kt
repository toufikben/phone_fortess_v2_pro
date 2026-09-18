package com.phonefortress.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.phonefortress.app.data.crypto.PinHasher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private val Context.pinDataStore by preferencesDataStore(name = "pin_prefs")

@Singleton
class PinPrefs @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hasher: PinHasher
) {
    private val verifyMutex = Mutex()
    private object Keys {
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val FAILED_ATTEMPTS = intPreferencesKey("pin_failed_attempts")
        val LOCKOUT_UNTIL = longPreferencesKey("pin_lockout_until")
        val LAST_UNLOCK = longPreferencesKey("last_unlock_at")
    }

    val isPinEnabled: Flow<Boolean> = context.pinDataStore.data.map { it[Keys.PIN_ENABLED] ?: false }
    val isBiometricEnabled: Flow<Boolean> = context.pinDataStore.data.map { it[Keys.BIOMETRIC_ENABLED] ?: false }
    val failedAttempts: Flow<Int> = context.pinDataStore.data.map { it[Keys.FAILED_ATTEMPTS] ?: 0 }
    val lockoutUntil: Flow<Long> = context.pinDataStore.data.map { it[Keys.LOCKOUT_UNTIL] ?: 0L }
    val lastUnlock: Flow<Long> = context.pinDataStore.data.map { it[Keys.LAST_UNLOCK] ?: 0L }

    suspend fun setPin(pin: String) {
        val hashed = hasher.hash(pin)
        context.pinDataStore.edit {
            it[Keys.PIN_HASH] = hashed.hash
            it[Keys.PIN_SALT] = hashed.salt
            it[Keys.PIN_ENABLED] = true
            it[Keys.FAILED_ATTEMPTS] = 0
            it[Keys.LOCKOUT_UNTIL] = 0L
        }
    }

    suspend fun verifyPin(pin: String): VerifyResult = verifyMutex.withLock {
        val prefs = context.pinDataStore.data.first()
        val hash = prefs[Keys.PIN_HASH] ?: return VerifyResult.NotSet
        val salt = prefs[Keys.PIN_SALT] ?: return VerifyResult.NotSet
        val lockoutUntil = prefs[Keys.LOCKOUT_UNTIL] ?: 0L
        if (lockoutUntil > System.currentTimeMillis()) return VerifyResult.LockedOut(lockoutUntil)
        val valid = hasher.verify(pin, PinHasher.HashedPin(hash, salt))
        return if (valid) {
            context.pinDataStore.edit {
                it[Keys.FAILED_ATTEMPTS] = 0
                it[Keys.LOCKOUT_UNTIL] = 0L
                it[Keys.LAST_UNLOCK] = System.currentTimeMillis()
            }
            VerifyResult.Success
        } else {
            val attempts = (prefs[Keys.FAILED_ATTEMPTS] ?: 0) + 1
            val lockoutMs = computeLockout(attempts)
            context.pinDataStore.edit {
                it[Keys.FAILED_ATTEMPTS] = attempts
                if (lockoutMs > 0) it[Keys.LOCKOUT_UNTIL] = System.currentTimeMillis() + lockoutMs
            }
            if (lockoutMs > 0) VerifyResult.LockedOut(System.currentTimeMillis() + lockoutMs)
            else VerifyResult.WrongPin((5 - attempts).coerceAtLeast(0))
        }
    }

    private fun computeLockout(attempts: Int): Long = when {
        attempts >= 10 -> 30 * 60_000L
        attempts >= 7 -> 5 * 60_000L
        attempts >= 5 -> 60_000L
        else -> 0L
    }

    suspend fun setBiometricEnabled(enabled: Boolean) { context.pinDataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled } }
    suspend fun disablePin() { context.pinDataStore.edit { it[Keys.PIN_ENABLED] = false; it.remove(Keys.PIN_HASH); it.remove(Keys.PIN_SALT); it[Keys.FAILED_ATTEMPTS] = 0; it[Keys.LOCKOUT_UNTIL] = 0L } }
    suspend fun markUnlocked() { context.pinDataStore.edit { it[Keys.LAST_UNLOCK] = System.currentTimeMillis() } }

    sealed class VerifyResult {
        data object Success : VerifyResult()
        data object NotSet : VerifyResult()
        data class WrongPin(val remainingAttempts: Int) : VerifyResult()
        data class LockedOut(val until: Long) : VerifyResult()
    }
}
