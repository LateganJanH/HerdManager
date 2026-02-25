package com.herdmanager.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.herdmanager.app.domain.model.ThemeMode
import com.herdmanager.app.domain.repository.ThemePreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_preferences"
)

private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")

private fun String.toThemeMode(): ThemeMode = when (this) {
    "light" -> ThemeMode.LIGHT
    "dark" -> ThemeMode.DARK
    else -> ThemeMode.SYSTEM
}

private fun ThemeMode.toStorage(): String = when (this) {
    ThemeMode.LIGHT -> "light"
    ThemeMode.DARK -> "dark"
    ThemeMode.SYSTEM -> "system"
}

class ThemePreferencesRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ThemePreferencesRepository {

    override fun themeMode(): Flow<ThemeMode> =
        context.appPreferencesDataStore.data.map { prefs ->
            (prefs[KEY_THEME_MODE] ?: "system").toThemeMode()
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.toStorage()
        }
    }
}
