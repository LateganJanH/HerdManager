package com.herdmanager.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herdmanager.app.domain.model.AnimalStatus
import com.herdmanager.app.domain.model.FarmSettings
import com.herdmanager.app.domain.model.PregnancyCheckResult
import com.herdmanager.app.domain.model.Sex
import com.herdmanager.app.domain.repository.AnimalRepository
import com.herdmanager.app.domain.repository.BreedingEventRepository
import com.herdmanager.app.domain.repository.CalvingEventRepository
import com.herdmanager.app.domain.repository.FarmSettingsRepository
import com.herdmanager.app.domain.repository.HealthEventRepository
import com.herdmanager.app.domain.repository.SyncRepository
import com.herdmanager.app.domain.repository.WeightRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private const val PREGNANCY_CHECK_WINDOW_DAYS = 14L
private const val WITHDRAWAL_WINDOW_DAYS = 14L
private const val WEANING_ALERT_WINDOW_DAYS = 14L
private const val WEANING_OVERDUE_DAYS = 30L
private const val DUE_SOON_PREVIEW_MAX = 3

private fun defaultHerdSummary() = HerdSummary(
    totalAnimals = 0,
    byStatus = AnimalStatus.entries.associateWith { 0 },
    bySex = Sex.entries.associateWith { 0 },
    calvingsThisYear = 0,
    openBreedingEvents = 0,
    dueSoonCount = 0,
    dueSoonPreview = emptyList()
)

/** One line for Home "due soon" card: e.g. "Cow 123 – Calving in 3 days". */
data class DueSoonPreviewItem(
    val damEarTag: String,
    val label: String,
    val animalId: String
)

data class HerdSummary(
    val totalAnimals: Int,
    val byStatus: Map<AnimalStatus, Int>,
    val bySex: Map<Sex, Int>,
    val calvingsThisYear: Int,
    val openBreedingEvents: Int,
    /** Count of calving due + pregnancy check due + withdrawal due in their alert windows (for Home "Due soon"). */
    val dueSoonCount: Int = 0,
    /** First few due items for Home card preview (ear tag + label). */
    val dueSoonPreview: List<DueSoonPreviewItem> = emptyList()
)

@HiltViewModel
class HerdSummaryViewModel @Inject constructor(
    private val animalRepository: AnimalRepository,
    private val breedingEventRepository: BreedingEventRepository,
    private val calvingEventRepository: CalvingEventRepository,
    private val farmSettingsRepository: FarmSettingsRepository,
    private val healthEventRepository: HealthEventRepository,
    private val weightRecordRepository: WeightRecordRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _summary = MutableStateFlow<HerdSummary>(defaultHerdSummary())
    val summary: StateFlow<HerdSummary> = _summary.asStateFlow()

    val lastSyncedAt = syncRepository.lastSyncedAt().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    fun syncNow() {
        viewModelScope.launch {
            _syncError.value = null
            _isSyncing.value = true
            syncRepository.syncNow()
                .onSuccess {
                    _isSyncing.value = false
                    loadSummary()
                }
                .onFailure { e ->
                    _isSyncing.value = false
                    _syncError.value = e.message ?: "Sync failed"
                }
        }
    }

    fun clearSyncError() {
        _syncError.value = null
    }

    fun formatLastSynced(instant: Instant?): String =
        if (instant == null) "Never"
        else java.time.format.DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())
            .withLocale(Locale.getDefault())
            .format(instant)

    init {
        loadSummary()
    }

    fun loadSummary() {
        viewModelScope.launch {
            runCatching {
                val settings = farmSettingsRepository.farmSettings().first()
                val animals = animalRepository.observeAnimalsByFarm(FarmSettings.DEFAULT_FARM_ID).first()
                val breedingEvents = breedingEventRepository.observeAllBreedingEvents().first()
                val calvingEvents = calvingEventRepository.getAllCalvingEvents()
                val calvedBreedingIds = calvingEvents.map { it.breedingEventId }.toSet()

                val byStatus = animals.groupingBy { it.status }.eachCount()
                val bySex = animals.groupingBy { it.sex }.eachCount()
                val year = LocalDate.now().year
                val calvingsThisYear = calvingEvents.count { it.actualDate.year == year }
                val openBreedingEvents = breedingEvents.count { event ->
                    event.id !in calvedBreedingIds && event.pregnancyCheckResult != PregnancyCheckResult.NOT_PREGNANT
                }

                val calvingAlertDays = settings.calvingAlertDaysClamped().toLong()
                val pregnancyCheckDays = settings.pregnancyCheckDaysClamped().toLong()
                val gestation = settings.gestationDaysClamped().toLong()
                val now = LocalDate.now()
                val calvingDueCount = breedingEvents.count { event ->
                    val due = event.dueDate(gestation.toInt())
                    event.id !in calvedBreedingIds &&
                        due >= now &&
                        due <= now.plusDays(calvingAlertDays)
                }
                val pregnancyCheckDueCount = breedingEvents.count { event ->
                    event.id !in calvedBreedingIds &&
                        !event.hasPregnancyCheck &&
                        event.serviceDate.plusDays(pregnancyCheckDays) in now..now.plusDays(PREGNANCY_CHECK_WINDOW_DAYS)
                }
                val healthEvents = healthEventRepository.observeAllHealthEvents().first()
                val withdrawalDueCount = healthEvents.count { it.withdrawalPeriodEnd != null && it.withdrawalPeriodEnd!! in now..now.plusDays(WITHDRAWAL_WINDOW_DAYS) }
                val weaningDays = settings.weaningAgeDaysClamped().toLong()
                val windowStart = now.minusDays(WEANING_OVERDUE_DAYS)
                val windowEnd = now.plusDays(WEANING_ALERT_WINDOW_DAYS)
                val allWeights = weightRecordRepository.observeAllWeightRecords().first()
                val weightsByAnimal = allWeights.groupBy { it.animalId }
                val weaningDueCount = animals.count { animal ->
                    val weaningDue = animal.dateOfBirth.plusDays(weaningDays)
                    weaningDue in windowStart..windowEnd &&
                        !weightsByAnimal[animal.id].orEmpty().any { it.date >= weaningDue.minusDays(WEANING_ALERT_WINDOW_DAYS) }
                }
                val dueSoonCount = calvingDueCount + pregnancyCheckDueCount + withdrawalDueCount + weaningDueCount

                val animalMap = animals.associateBy { it.id }
                val calvingPreview = breedingEvents
                    .filter { it.id !in calvedBreedingIds }
                    .map { event ->
                        val due = event.dueDate(gestation.toInt())
                        event to due
                    }
                    .filter { (_, due) -> due >= now && due <= now.plusDays(calvingAlertDays) }
                    .map { (event, due) ->
                        val days = ChronoUnit.DAYS.between(now, due)
                        val label = when {
                            days < 0 -> "Calving overdue by ${-days} days"
                            days == 0L -> "Calving due today"
                            else -> "Calving in $days days"
                        }
                        due to DueSoonPreviewItem(
                            damEarTag = animalMap[event.animalId]?.earTagNumber ?: "Unknown",
                            label = label,
                            animalId = event.animalId
                        )
                    }
                val pregnancyPreview = breedingEvents
                    .filter { it.id !in calvedBreedingIds && !it.hasPregnancyCheck }
                    .map { event ->
                        val checkDue = event.serviceDate.plusDays(pregnancyCheckDays)
                        event to checkDue
                    }
                    .filter { (_, checkDue) -> checkDue in now..now.plusDays(PREGNANCY_CHECK_WINDOW_DAYS) }
                    .map { (event, checkDue) ->
                        val days = ChronoUnit.DAYS.between(now, checkDue)
                        val label = when {
                            days < 0 -> "Pregnancy check overdue by ${-days} days"
                            days == 0L -> "Pregnancy check due today"
                            else -> "Pregnancy check in $days days"
                        }
                        checkDue to DueSoonPreviewItem(
                            damEarTag = animalMap[event.animalId]?.earTagNumber ?: "Unknown",
                            label = label,
                            animalId = event.animalId
                        )
                    }
                val withdrawalPreview = healthEvents
                    .filter { it.withdrawalPeriodEnd != null }
                    .mapNotNull { event ->
                        val end = event.withdrawalPeriodEnd!!
                        if (end in now..now.plusDays(WITHDRAWAL_WINDOW_DAYS)) {
                            val days = ChronoUnit.DAYS.between(now, end)
                            val label = when {
                                days < 0 -> "Withdrawal overdue by ${-days} days"
                                days == 0L -> "Withdrawal ends today"
                                else -> "Withdrawal ends in $days days"
                            }
                            end to DueSoonPreviewItem(
                                damEarTag = animalMap[event.animalId]?.earTagNumber ?: "Unknown",
                                label = label,
                                animalId = event.animalId
                            )
                        } else null
                    }
                val weaningPreview = animals
                    .mapNotNull { animal ->
                        val weaningDue = animal.dateOfBirth.plusDays(weaningDays)
                        if (weaningDue in windowStart..windowEnd) {
                            val hasWeight = weightsByAnimal[animal.id].orEmpty().any { it.date >= weaningDue.minusDays(WEANING_ALERT_WINDOW_DAYS) }
                            if (!hasWeight) {
                                val days = ChronoUnit.DAYS.between(now, weaningDue)
                                val label = when {
                                    days < 0 -> "Weaning weight overdue by ${-days} days"
                                    days == 0L -> "Weaning weight due today"
                                    else -> "Weaning weight in $days days"
                                }
                                weaningDue to DueSoonPreviewItem(
                                    damEarTag = animal.earTagNumber,
                                    label = label,
                                    animalId = animal.id
                                )
                            } else null
                        } else null
                    }
                val dueSoonPreview = (calvingPreview + pregnancyPreview + withdrawalPreview + weaningPreview)
                    .sortedBy { it.first }
                    .take(DUE_SOON_PREVIEW_MAX)
                    .map { it.second }

                HerdSummary(
                    totalAnimals = animals.size,
                    byStatus = AnimalStatus.entries.associateWith { byStatus[it] ?: 0 },
                    bySex = Sex.entries.associateWith { bySex[it] ?: 0 },
                    calvingsThisYear = calvingsThisYear,
                    openBreedingEvents = openBreedingEvents,
                    dueSoonCount = dueSoonCount,
                    dueSoonPreview = dueSoonPreview
                )
            }.fold(
                onSuccess = { _summary.value = it },
                onFailure = { _summary.value = defaultHerdSummary() }
            )
        }
    }
}
