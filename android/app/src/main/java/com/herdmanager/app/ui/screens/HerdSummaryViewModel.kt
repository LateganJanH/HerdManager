package com.herdmanager.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herdmanager.app.domain.model.Animal
import com.herdmanager.app.domain.model.AnimalStatus
import com.herdmanager.app.domain.model.isInCurrentHerd
import com.herdmanager.app.domain.model.FarmSettings
import com.herdmanager.app.domain.model.PregnancyCheckResult
import com.herdmanager.app.domain.model.Sex
import com.herdmanager.app.domain.repository.AnimalRepository
import com.herdmanager.app.domain.repository.BreedingEventRepository
import com.herdmanager.app.domain.repository.CalvingEventRepository
import com.herdmanager.app.domain.repository.FarmSettingsRepository
import com.herdmanager.app.domain.repository.HealthEventRepository
import com.herdmanager.app.domain.repository.ConditionRecordRepository
import com.herdmanager.app.domain.repository.SyncRepository
import com.herdmanager.app.domain.repository.WeightRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
private const val OPEN_COW_NO_BREEDING_DAYS = 60L
private const val AT_RISK_PREVIEW_MAX = 5
private const val CALF_AGE_MONTHS = 12L
private const val HEIFER_AGE_MONTHS = 24L

/** Category labels in display order; matches Herd list filter and web byCategory. */
val HERD_CATEGORY_ORDER = listOf("Calves", "Heifers", "Cows", "Bulls", "Steers")

private fun Animal.categoryForSummary(): String {
    val monthsOld = ChronoUnit.MONTHS.between(dateOfBirth, LocalDate.now())
    return when {
        monthsOld < CALF_AGE_MONTHS -> "Calves"
        sex == Sex.FEMALE -> if (monthsOld >= HEIFER_AGE_MONTHS) "Cows" else "Heifers"
        isCastrated == true -> "Steers"
        else -> "Bulls"
    }
}

private fun defaultHerdSummary() = HerdSummary(
    totalAnimals = 0,
    byStatus = AnimalStatus.entries.associateWith { 0 },
    bySex = Sex.entries.associateWith { 0 },
    byCategory = HERD_CATEGORY_ORDER.associateWith { 0 },
    calvingsThisYear = 0,
    openBreedingEvents = 0,
    dueSoonCount = 0,
    dueSoonPreview = emptyList(),
    atRiskPreview = emptyList(),
    avgDailyGainAll = null,
    avgDailyGainBySex = emptyMap(),
    avgWeaningWeightKg = null,
    avgConditionScore = null,
    bcsDistribution = emptyMap()
)

/** One line for Home "due soon" card: e.g. "Cow 123 – Calving in 3 days". */
data class DueSoonPreviewItem(
    val damEarTag: String,
    val label: String,
    val animalId: String
)

/** One line for Home "Attention needed" card: at-risk animal and reason. */
data class AtRiskItem(
    val animalId: String,
    val earTag: String,
    val reason: String
)

data class HerdSummary(
    val totalAnimals: Int,
    val byStatus: Map<AnimalStatus, Int>,
    val bySex: Map<Sex, Int>,
    /** Calves, Heifers, Cows, Bulls, Steers – matches Herd list and web; Bulls = male not castrated, Steers = male castrated. */
    val byCategory: Map<String, Int> = emptyMap(),
    val calvingsThisYear: Int,
    val openBreedingEvents: Int,
    /** Count of calving due + pregnancy check due + withdrawal due in their alert windows (for Home "Due soon"). */
    val dueSoonCount: Int = 0,
    /** First few due items for Home card preview (ear tag + label). */
    val dueSoonPreview: List<DueSoonPreviewItem> = emptyList(),
    /** At-risk / recommended actions: open cows (no breeding in N days), overdue weaning weight, etc. */
    val atRiskPreview: List<AtRiskItem> = emptyList(),
    /** Average daily gain (kg/day) across all animals with at least two weight records. */
    val avgDailyGainAll: Double? = null,
    /** Average daily gain (kg/day) grouped by sex. */
    val avgDailyGainBySex: Map<Sex, Double?> = emptyMap(),
    /** Average weaning weight (kg) for calves with a recorded weight near weaning. */
    val avgWeaningWeightKg: Double? = null,
    /** Average body condition score (1–9) across all condition records. */
    val avgConditionScore: Double? = null,
    /** Count of condition records per score (1–9) for current herd; keys 1..9, values ≥ 0. */
    val bcsDistribution: Map<Int, Int> = emptyMap()
)

@HiltViewModel
class HerdSummaryViewModel @Inject constructor(
    private val animalRepository: AnimalRepository,
    private val breedingEventRepository: BreedingEventRepository,
    private val calvingEventRepository: CalvingEventRepository,
    private val farmSettingsRepository: FarmSettingsRepository,
    private val healthEventRepository: HealthEventRepository,
    private val weightRecordRepository: WeightRecordRepository,
    private val conditionRecordRepository: ConditionRecordRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _summary = MutableStateFlow<HerdSummary>(defaultHerdSummary())
    val summary: StateFlow<HerdSummary> = _summary.asStateFlow()

    val lastSyncedAt = syncRepository.lastSyncedAt().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    val farmDisplayName = farmSettingsRepository.farmSettings()
        .map { it.displayName }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "My Farm"
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
                    _syncError.value = e.message?.takeIf { it.isNotBlank() }
                        ?: e.javaClass.simpleName.takeIf { it.isNotBlank() }
                        ?: "Sync failed"
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

                val activeAnimalsForSummary = animals.filter { it.isInCurrentHerd }
                val activeAnimalIds = activeAnimalsForSummary.map { it.id }.toSet()
                val byStatus = activeAnimalsForSummary.groupingBy { it.status }.eachCount()
                val bySex = activeAnimalsForSummary.groupingBy { it.sex }.eachCount()
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
                    event.animalId in activeAnimalIds &&
                        event.id !in calvedBreedingIds && run {
                        val due = event.dueDate(gestation.toInt())
                        due >= now && due <= now.plusDays(calvingAlertDays)
                    }
                }
                val pregnancyCheckDueCount = breedingEvents.count { event ->
                    event.animalId in activeAnimalIds &&
                        event.id !in calvedBreedingIds &&
                        !event.hasPregnancyCheck &&
                        event.serviceDate.plusDays(pregnancyCheckDays) in now..now.plusDays(PREGNANCY_CHECK_WINDOW_DAYS)
                }
                val healthEvents = healthEventRepository.observeAllHealthEvents().first()
                val withdrawalDueCount = healthEvents.count { event ->
                    event.animalId in activeAnimalIds &&
                        event.withdrawalPeriodEnd != null &&
                        event.withdrawalPeriodEnd!! in now..now.plusDays(WITHDRAWAL_WINDOW_DAYS)
                }
                val weaningDays = settings.weaningAgeDaysClamped().toLong()
                val windowStart = now.minusDays(WEANING_OVERDUE_DAYS)
                val windowEnd = now.plusDays(WEANING_ALERT_WINDOW_DAYS)
                val allWeights = weightRecordRepository.observeAllWeightRecords().first()
                val weightsByAnimal = allWeights.groupBy { it.animalId }
                val weaningDueCount = animals.count { animal ->
                    animal.isInCurrentHerd &&
                        run {
                            val weaningDue = animal.dateOfBirth.plusDays(weaningDays)
                            weaningDue in windowStart..windowEnd &&
                                !weightsByAnimal[animal.id].orEmpty().any { it.date >= weaningDue.minusDays(WEANING_ALERT_WINDOW_DAYS) }
                        }
                }
                val dueSoonCount = calvingDueCount + pregnancyCheckDueCount + withdrawalDueCount + weaningDueCount

                val animalMap = animals.associateBy { it.id }
                val calvingPreview = breedingEvents
                    .filter { it.animalId in activeAnimalIds && it.id !in calvedBreedingIds }
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
                    .filter { it.animalId in activeAnimalIds && it.id !in calvedBreedingIds && !it.hasPregnancyCheck }
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
                    .filter { it.animalId in activeAnimalIds && it.withdrawalPeriodEnd != null }
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
                    .filter { it.isInCurrentHerd }
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

                // At-risk / recommended actions: open cows (no breeding in N days), overdue weaning
                val breedingByAnimal = breedingEvents.filter { it.animalId in activeAnimalIds }
                    .groupBy { it.animalId }
                    .mapValues { (_, events) -> events.maxByOrNull { it.serviceDate }?.serviceDate }
                val openCowsAtRisk = activeAnimalsForSummary
                    .filter { it.sex == Sex.FEMALE && (it.categoryForSummary() == "Cows" || it.categoryForSummary() == "Heifers") }
                    .filter { animal ->
                        val lastBreeding = breedingByAnimal[animal.id]
                        lastBreeding == null || ChronoUnit.DAYS.between(lastBreeding, now) > OPEN_COW_NO_BREEDING_DAYS
                    }
                    .map { AtRiskItem(it.id, it.earTagNumber, "No breeding in $OPEN_COW_NO_BREEDING_DAYS days") }
                val overdueWeaningAtRisk = activeAnimalsForSummary
                    .mapNotNull { animal ->
                        val weaningDue = animal.dateOfBirth.plusDays(weaningDays)
                        if (weaningDue >= now) return@mapNotNull null
                        val hasWeight = weightsByAnimal[animal.id].orEmpty()
                            .any { it.date >= weaningDue.minusDays(WEANING_ALERT_WINDOW_DAYS) }
                        if (!hasWeight) AtRiskItem(animal.id, animal.earTagNumber, "Weaning weight overdue")
                        else null
                    }

                // Growth KPIs (current herd only – exclude SOLD)
                val gainPerDayByAnimalId: Map<String, Double> =
                    weightsByAnimal.mapNotNull { (animalId, records) ->
                        val summary = computeGrowthSummary(records)
                        summary?.gainPerDayKg?.let { gain -> animalId to gain }
                    }.toMap()

                val allGains = activeAnimalsForSummary.mapNotNull { gainPerDayByAnimalId[it.id] }
                val avgDailyGainAll = if (allGains.isNotEmpty()) allGains.average() else null

                val avgDailyGainBySex: Map<Sex, Double?> =
                    Sex.entries.associateWith { sex ->
                        val gainsForSex = activeAnimalsForSummary
                            .filter { it.sex == sex }
                            .mapNotNull { gainPerDayByAnimalId[it.id] }
                        if (gainsForSex.isNotEmpty()) gainsForSex.average() else null
                    }

                // Low-growth at risk: animals with gain per day significantly below herd average.
                val lowGrowthAtRisk: List<AtRiskItem> = buildList {
                    val target = avgDailyGainAll?.times(0.7)
                    if (target != null && target > 0) {
                        activeAnimalsForSummary.forEach { animal ->
                            val gain = gainPerDayByAnimalId[animal.id] ?: return@forEach
                            if (gain < target) {
                                add(
                                    AtRiskItem(
                                        animalId = animal.id,
                                        earTag = animal.earTagNumber,
                                        reason = "Low growth (%.2f kg/day)".format(Locale.getDefault(), gain)
                                    )
                                )
                            }
                        }
                    }
                }
                val atRiskPreview = (openCowsAtRisk + overdueWeaningAtRisk + lowGrowthAtRisk)
                    .take(AT_RISK_PREVIEW_MAX)

                // Weaning weights for calves whose weaning date has passed (current herd only)
                val weaningWeights = mutableListOf<Double>()
                activeAnimalsForSummary.forEach { animal ->
                    val weaningDue = animal.dateOfBirth.plusDays(weaningDays)
                    if (!weaningDue.isAfter(now)) {
                        val records = weightsByAnimal[animal.id].orEmpty()
                        if (records.isNotEmpty()) {
                            val cutoff = weaningDue.plusDays(WEANING_ALERT_WINDOW_DAYS)
                            val candidates = records.filter { !it.date.isAfter(cutoff) }
                            if (candidates.isNotEmpty()) {
                                val closest = candidates.minByOrNull {
                                    kotlin.math.abs(ChronoUnit.DAYS.between(weaningDue, it.date))
                                }!!
                                weaningWeights += closest.weightKg
                            }
                        }
                    }
                }
                val avgWeaningWeightKg = if (weaningWeights.isNotEmpty()) weaningWeights.average() else null

                // Condition scoring – average for current herd only (records for non-SOLD animals)
                val activeIds = activeAnimalsForSummary.map { it.id }.toSet()
                val allConditions = conditionRecordRepository.observeAll().first()
                    .filter { it.animalId in activeIds }
                val avgConditionScore = if (allConditions.isNotEmpty()) {
                    allConditions.map { it.score }.average()
                } else null
                val bcsDistribution = (1..9).associateWith { score ->
                    allConditions.count { it.score == score }
                }

                val byCategory = activeAnimalsForSummary
                    .groupingBy { it.categoryForSummary() }
                    .eachCount()
                    .let { counts -> HERD_CATEGORY_ORDER.associateWith { counts[it] ?: 0 } }

                HerdSummary(
                    totalAnimals = animals.count { it.isInCurrentHerd },
                    byStatus = AnimalStatus.entries.associateWith { byStatus[it] ?: 0 },
                    bySex = Sex.entries.associateWith { bySex[it] ?: 0 },
                    byCategory = byCategory,
                    calvingsThisYear = calvingsThisYear,
                    openBreedingEvents = openBreedingEvents,
                    dueSoonCount = dueSoonCount,
                    dueSoonPreview = dueSoonPreview,
                    atRiskPreview = atRiskPreview,
                    avgDailyGainAll = avgDailyGainAll,
                    avgDailyGainBySex = avgDailyGainBySex,
                    avgWeaningWeightKg = avgWeaningWeightKg,
                    avgConditionScore = avgConditionScore,
                    bcsDistribution = bcsDistribution
                )
            }.fold(
                onSuccess = { _summary.value = it },
                onFailure = { _summary.value = defaultHerdSummary() }
            )
        }
    }
}
