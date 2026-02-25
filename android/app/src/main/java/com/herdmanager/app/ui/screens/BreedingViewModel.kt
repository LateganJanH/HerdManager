package com.herdmanager.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herdmanager.app.domain.model.BreedingEvent
import com.herdmanager.app.domain.model.FarmSettings
import com.herdmanager.app.domain.model.HealthEvent
import com.herdmanager.app.domain.repository.AnimalRepository
import com.herdmanager.app.domain.repository.BreedingEventRepository
import com.herdmanager.app.domain.repository.CalvingEventRepository
import com.herdmanager.app.domain.repository.FarmSettingsRepository
import com.herdmanager.app.domain.repository.HealthEventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class GestationListItem(
    val event: BreedingEvent,
    val damEarTag: String,
    val daysUntilDue: Long,
    val dueDate: LocalDate
)

data class PregnancyCheckDueItem(
    val event: BreedingEvent,
    val damEarTag: String,
    val checkDueDate: LocalDate,
    val daysUntilDue: Long
)

data class WithdrawalDueItem(
    val event: HealthEvent,
    val earTag: String,
    val endDate: LocalDate,
    val daysUntilDue: Long
)

private const val PREGNANCY_CHECK_WINDOW_DAYS = 14L
private const val WITHDRAWAL_WINDOW_DAYS = 14L

@HiltViewModel
class BreedingViewModel @Inject constructor(
    private val breedingEventRepository: BreedingEventRepository,
    private val animalRepository: AnimalRepository,
    private val calvingEventRepository: CalvingEventRepository,
    private val farmSettingsRepository: FarmSettingsRepository,
    private val healthEventRepository: HealthEventRepository
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0)

    val upcomingCalvings = combine(
        refreshTrigger,
        farmSettingsRepository.farmSettings(),
        breedingEventRepository.observeAllBreedingEvents(),
        animalRepository.observeAnimalsByFarm(FarmSettings.DEFAULT_FARM_ID),
        calvingEventRepository.observeCalvedBreedingEventIds()
    ) { _, settings, events, animals, calvedIds ->
        val gestation = settings.gestationDaysClamped()
        val calvedSet = calvedIds.toSet()
        val animalMap = animals.associateBy { it.id }
        events
            .filter { it.id !in calvedSet && it.dueDate(gestation) >= LocalDate.now().minusDays(30) }
            .sortedBy { it.dueDate(gestation) }
            .map { event ->
                val dam = animalMap[event.animalId]
                val due = event.dueDate(gestation)
                GestationListItem(
                    event = event,
                    damEarTag = dam?.earTagNumber ?: "Unknown",
                    daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), due),
                    dueDate = due
                )
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val pregnancyCheckDue = combine(
        refreshTrigger,
        farmSettingsRepository.farmSettings(),
        breedingEventRepository.observeAllBreedingEvents(),
        animalRepository.observeAnimalsByFarm(FarmSettings.DEFAULT_FARM_ID),
        calvingEventRepository.observeCalvedBreedingEventIds()
    ) { _, settings, events, animals, calvedIds ->
        val checkDays = settings.pregnancyCheckDaysClamped().toLong()
        val calvedSet = calvedIds.toSet()
        val animalMap = animals.associateBy { it.id }
        events
            .filter { it.id !in calvedSet && !it.hasPregnancyCheck }
            .map { event ->
                val checkDue = event.serviceDate.plusDays(checkDays)
                event to checkDue
            }
            .filter { (_, checkDue) ->
                checkDue in LocalDate.now()..LocalDate.now().plusDays(PREGNANCY_CHECK_WINDOW_DAYS)
            }
            .sortedBy { (_, checkDue) -> checkDue }
            .map { (event, checkDue) ->
                val dam = animalMap[event.animalId]
                PregnancyCheckDueItem(
                    event = event,
                    damEarTag = dam?.earTagNumber ?: "Unknown",
                    checkDueDate = checkDue,
                    daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), checkDue)
                )
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val withdrawalDue = combine(
        refreshTrigger,
        healthEventRepository.observeAllHealthEvents(),
        animalRepository.observeAnimalsByFarm(FarmSettings.DEFAULT_FARM_ID)
    ) { _, healthEvents, animals ->
        val now = LocalDate.now()
        val endRange = now.plusDays(WITHDRAWAL_WINDOW_DAYS)
        val animalMap = animals.associateBy { it.id }
        healthEvents
            .filter { it.withdrawalPeriodEnd != null }
            .mapNotNull { event ->
                val end = event.withdrawalPeriodEnd!! 
                if (end in now..endRange) {
                    val earTag = animalMap[event.animalId]?.earTagNumber ?: "Unknown"
                    WithdrawalDueItem(
                        event = event,
                        earTag = earTag,
                        endDate = end,
                        daysUntilDue = ChronoUnit.DAYS.between(now, end)
                    )
                } else null
            }
            .sortedBy { it.endDate }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun refresh() {
        refreshTrigger.value++
    }
}
