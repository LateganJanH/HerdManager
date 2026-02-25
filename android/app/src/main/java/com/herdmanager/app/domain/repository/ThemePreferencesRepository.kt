package com.herdmanager.app.domain.repository

import com.herdmanager.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface ThemePreferencesRepository {
    fun themeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
