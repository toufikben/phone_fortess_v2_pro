package com.phonefortress.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.phonefortress.app.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.securityDataStore by preferencesDataStore(name = "security_prefs")

/**
 * إعدادات الحماية الأساسية.
 */
@Singleton
class SecurityPrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class AttemptEvaluation(
        val newAttemptCount: Int,
        val thresholdReached: Boolean
    )

    private object Keys {
        val PROTECTION_ENABLED = booleanPreferencesKey("protection_enabled")
        val THRESHOLD = intPreferencesKey("threshold")
        val CAPTURE_PHOTO = booleanPreferencesKey("capture_photo")
        val CAPTURE_AUDIO = booleanPreferencesKey("capture_audio")
        val CAPTURE_LOCATION = booleanPreferencesKey("capture_location")
        val RETENTION_DAYS = intPreferencesKey("retention_days")
        val CONSECUTIVE_ATTEMPTS = intPreferencesKey("consecutive_attempts")
        val LAST_EVENT_AT = stringPreferencesKey("last_event_at")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val protectionEnabled: Flow<Boolean> =
        context.securityDataStore.data.map { it[Keys.PROTECTION_ENABLED] ?: false }

    suspend fun setProtectionEnabled(enabled: Boolean) {
        context.securityDataStore.edit { it[Keys.PROTECTION_ENABLED] = enabled }
    }

    val threshold: Flow<Int> =
        context.securityDataStore.data.map {
            (it[Keys.THRESHOLD] ?: Constants.DEFAULT_THRESHOLD)
                .coerceIn(Constants.MIN_THRESHOLD, Constants.MAX_THRESHOLD)
        }

    suspend fun setThreshold(value: Int) {
        context.securityDataStore.edit { it[Keys.THRESHOLD] = value.coerceIn(Constants.MIN_THRESHOLD, Constants.MAX_THRESHOLD) }
    }

    val capturePhoto: Flow<Boolean> =
        context.securityDataStore.data.map { it[Keys.CAPTURE_PHOTO] ?: true }

    suspend fun setCapturePhoto(enabled: Boolean) {
        context.securityDataStore.edit { it[Keys.CAPTURE_PHOTO] = enabled }
    }

    val captureAudio: Flow<Boolean> =
        context.securityDataStore.data.map { it[Keys.CAPTURE_AUDIO] ?: false }

    suspend fun setCaptureAudio(enabled: Boolean) {
        context.securityDataStore.edit { it[Keys.CAPTURE_AUDIO] = enabled }
    }

    val captureLocation: Flow<Boolean> =
        context.securityDataStore.data.map { it[Keys.CAPTURE_LOCATION] ?: true }

    suspend fun setCaptureLocation(enabled: Boolean) {
        context.securityDataStore.edit { it[Keys.CAPTURE_LOCATION] = enabled }
    }

    val retentionDays: Flow<Int> =
        context.securityDataStore.data.map { it[Keys.RETENTION_DAYS] ?: Constants.DEFAULT_RETENTION_DAYS }

    suspend fun setRetentionDays(days: Int) {
        context.securityDataStore.edit { it[Keys.RETENTION_DAYS] = days.coerceIn(1, 90) }
    }

    val consecutiveAttempts: Flow<Int> =
        context.securityDataStore.data.map { it[Keys.CONSECUTIVE_ATTEMPTS] ?: 0 }

    /**
     * Atomically increments and consumes the threshold window when reached.
     * The returned count is the official count for the triggering event; a later
     * callback starts from zero and cannot be erased by a stale reset.
     */
    suspend fun incrementAttemptsAndCheckThreshold(threshold: Int): AttemptEvaluation {
        var evaluation: AttemptEvaluation? = null
        context.securityDataStore.edit { prefs ->
            val next = (prefs[Keys.CONSECUTIVE_ATTEMPTS] ?: 0) + 1
            val reached = next >= threshold.coerceAtLeast(1)
            prefs[Keys.CONSECUTIVE_ATTEMPTS] = if (reached) 0 else next
            evaluation = AttemptEvaluation(next, reached)
        }
        return requireNotNull(evaluation)
    }

    suspend fun resetAttempts() {
        context.securityDataStore.edit { it[Keys.CONSECUTIVE_ATTEMPTS] = 0 }
    }

    val language: Flow<String> =
        context.securityDataStore.data.map { it[Keys.LANGUAGE] ?: "ar" }

    suspend fun setLanguage(code: String) {
        context.securityDataStore.edit { it[Keys.LANGUAGE] = code }
    }
}
