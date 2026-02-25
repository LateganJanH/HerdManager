package com.herdmanager.app.domain.repository

import com.herdmanager.app.domain.model.FarmSettings
import kotlinx.coroutines.flow.Flow

interface FarmSettingsRepository {
    fun farmSettings(): Flow<FarmSettings>
    suspend fun updateFarmSettings(settings: FarmSettings)
}
