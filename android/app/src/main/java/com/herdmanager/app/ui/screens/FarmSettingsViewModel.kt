package com.herdmanager.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herdmanager.app.domain.model.FarmSettings
import com.herdmanager.app.domain.model.Herd
import com.herdmanager.app.domain.repository.BackupRepository
import com.herdmanager.app.domain.repository.FarmSettingsRepository
import com.herdmanager.app.domain.repository.HerdRepository
import com.herdmanager.app.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class FarmSettingsViewModel @Inject constructor(
    private val farmSettingsRepository: FarmSettingsRepository,
    private val herdRepository: HerdRepository,
    private val backupRepository: BackupRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    val lastSyncedAt = syncRepository.lastSyncedAt()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError = _syncError.asStateFlow()

    fun syncNow() {
        viewModelScope.launch {
            _syncError.value = null
            _isSyncing.value = true
            syncRepository.syncNow()
                .onSuccess { _isSyncing.value = false }
                .onFailure { e ->
                    _isSyncing.value = false
                    _syncError.value = e.message ?: "Sync failed"
                }
        }
    }

    fun clearSyncError() {
        _syncError.value = null
    }

    val farmSettings = farmSettingsRepository.farmSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FarmSettings()
        )

    val herds = herdRepository.observeHerdsByFarm(FarmSettings.DEFAULT_FARM_ID)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addHerd(name: String, description: String? = null) {
        viewModelScope.launch {
            herdRepository.insertHerd(
                Herd(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    farmId = FarmSettings.DEFAULT_FARM_ID,
                    description = description?.takeIf { it.isNotBlank() },
                    sortOrder = 0
                )
            )
        }
    }

    fun deleteHerd(herd: Herd) {
        viewModelScope.launch {
            herdRepository.deleteHerd(herd.id)
        }
    }

    fun updateFarmSettings(settings: FarmSettings) {
        viewModelScope.launch {
            farmSettingsRepository.updateFarmSettings(settings)
        }
    }

    suspend fun exportBackupJson(): String = backupRepository.exportToJson()

    suspend fun importFromJson(json: String): Result<Unit> = backupRepository.importFromJson(json)

    fun formatLastSynced(instant: Instant?): String =
        if (instant == null) "Never"
        else java.time.format.DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())
            .withLocale(Locale.getDefault())
            .format(instant)
}
