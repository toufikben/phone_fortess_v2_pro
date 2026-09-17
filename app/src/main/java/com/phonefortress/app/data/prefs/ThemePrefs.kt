package com.phonefortress.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.phonefortress.app.domain.model.AppTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

@Singleton
class ThemePrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyTheme = stringPreferencesKey("selected_theme")

    val theme: Flow<AppTheme> = context.themeDataStore.data.map { prefs ->
        AppTheme.fromId(prefs[keyTheme])
    }

    suspend fun setTheme(theme: AppTheme) {
        context.themeDataStore.edit { it[keyTheme] = theme.id }
    }
}
