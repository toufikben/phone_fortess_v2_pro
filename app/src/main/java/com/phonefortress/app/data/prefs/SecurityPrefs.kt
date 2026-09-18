package com.phonefortress.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.phonefortress.app.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val securityCorruptedKey = booleanPreferencesKey("corruption_detected")
private val Context.securityDataStore by preferencesDataStore(
    name = "security_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { preferencesOf(securityCorruptedKey to true) }
)

/**
 * إعدادات الحماية الأساسية.
 */
@Singleton
class SecurityPrefs private constructor(
    private val dataStore: DataStore<Preferences>,
    @Suppress("UNUSED_PARAMETER") marker: Unit
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.securityDataStore, Unit)

    internal constructor(dataStore: DataStore<Preferences>) : this(dataStore, Unit)

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
        val CURRENT_ZONE_ID = stringPreferencesKey("current_zone_id")
        val IN_SAFE_ZONE = booleanPreferencesKey("in_safe_zone")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val protectionEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.PROTECTION_ENABLED] ?: (it[securityCorruptedKey] == true) }

    suspend fun setProtectionEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.PROTECTION_ENABLED] = enabled }
    }

    val threshold: Flow<Int> =
        dataStore.data.map {
            (it[Keys.THRESHOLD] ?: if (it[securityCorruptedKey] == true) Constants.MIN_THRESHOLD else Constants.DEFAULT_THRESHOLD)
                .coerceIn(Constants.MIN_THRESHOLD, Constants.MAX_THRESHOLD)
        }

    suspend fun setThreshold(value: Int) {
        dataStore.edit { it[Keys.THRESHOLD] = value.coerceIn(Constants.MIN_THRESHOLD, Constants.MAX_THRESHOLD) }
    }

    val capturePhoto: Flow<Boolean> =
        dataStore.data.map { it[Keys.CAPTURE_PHOTO] ?: true }

    suspend fun setCapturePhoto(enabled: Boolean) {
        dataStore.edit { it[Keys.CAPTURE_PHOTO] = enabled }
    }

    val captureAudio: Flow<Boolean> =
        dataStore.data.map { it[Keys.CAPTURE_AUDIO] ?: false }

    suspend fun setCaptureAudio(enabled: Boolean) {
        dataStore.edit { it[Keys.CAPTURE_AUDIO] = enabled }
    }

    val captureLocation: Flow<Boolean> =
        dataStore.data.map { it[Keys.CAPTURE_LOCATION] ?: true }

    suspend fun setCaptureLocation(enabled: Boolean) {
        dataStore.edit { it[Keys.CAPTURE_LOCATION] = enabled }
    }

    val retentionDays: Flow<Int> =
        dataStore.data.map {
            (it[Keys.RETENTION_DAYS] ?: Constants.DEFAULT_RETENTION_DAYS).coerceIn(1, 90)
        }

    suspend fun setRetentionDays(days: Int) {
        dataStore.edit { it[Keys.RETENTION_DAYS] = days.coerceIn(1, 90) }
    }

    val consecutiveAttempts: Flow<Int> =
        dataStore.data.map { it[Keys.CONSECUTIVE_ATTEMPTS] ?: 0 }

    /**
     * Atomically increments and consumes the threshold window when reached.
     * The returned count is the official count for the triggering event; a later
     * callback starts from zero and cannot be erased by a stale reset.
     */
    suspend fun incrementAttemptsAndCheckThreshold(threshold: Int): AttemptEvaluation {
        var evaluation: AttemptEvaluation? = null
        dataStore.edit { prefs ->
            val next = (prefs[Keys.CONSECUTIVE_ATTEMPTS] ?: 0) + 1
            val reached = next >= threshold.coerceAtLeast(1)
            prefs[Keys.CONSECUTIVE_ATTEMPTS] = if (reached) 0 else next
            evaluation = AttemptEvaluation(next, reached)
        }
        return requireNotNull(evaluation)
    }

    suspend fun resetAttempts() {
        dataStore.edit { it[Keys.CONSECUTIVE_ATTEMPTS] = 0 }
    }

    val currentZoneId: Flow<String?> = dataStore.data.map { it[Keys.CURRENT_ZONE_ID] }
    val inSafeZone: Flow<Boolean?> = dataStore.data.map { it[Keys.IN_SAFE_ZONE] }

    suspend fun setCurrentZoneId(id: String?) {
        dataStore.edit {
            if (id == null) it.remove(Keys.CURRENT_ZONE_ID) else it[Keys.CURRENT_ZONE_ID] = id
        }
    }

    suspend fun setInSafeZone(value: Boolean?) {
        dataStore.edit {
            if (value == null) it.remove(Keys.IN_SAFE_ZONE) else it[Keys.IN_SAFE_ZONE] = value
        }
    }

    val language: Flow<String> =
        dataStore.data.map { it[Keys.LANGUAGE] ?: "ar" }

    suspend fun setLanguage(code: String) {
        dataStore.edit { it[Keys.LANGUAGE] = code }
    }
}
