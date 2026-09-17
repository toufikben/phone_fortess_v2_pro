package com.phonefortress.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.autoLockDataStore by preferencesDataStore(name = "auto_lock_prefs")

@Singleton
class AutoLockPrefs @Inject constructor(@ApplicationContext private val context: Context) {
    private val keyTimeout = intPreferencesKey("auto_lock_timeout_sec")
    val timeoutSeconds: Flow<Int> = context.autoLockDataStore.data.map { it[keyTimeout] ?: 60 }
    suspend fun setTimeout(seconds: Int) {
        context.autoLockDataStore.edit { it[keyTimeout] = seconds.coerceIn(0, 600) }
    }
}
