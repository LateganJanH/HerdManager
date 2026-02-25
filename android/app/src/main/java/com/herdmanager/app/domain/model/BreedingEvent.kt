package com.herdmanager.app.domain.model

import java.time.LocalDate

data class BreedingEvent(
    val id: String,
    val animalId: String,
    val sireIds: List<String> = emptyList(),
    val eventType: BreedingEventType,
    val serviceDate: LocalDate,
    val notes: String? = null,
    val pregnancyCheckDate: LocalDate? = null,
    val pregnancyCheckResult: PregnancyCheckResult? = null
) {
    /** Primary sire if only one; first candidate otherwise. For backward compat. */
    val primarySireId: String? get() = sireIds.firstOrNull()

    /** Due date using default gestation (cattle average). Prefer dueDate(gestationDays) with farm setting. */
    val dueDate: LocalDate get() = serviceDate.plusDays(DEFAULT_GESTATION_DAYS)

    /** Due date for a given gestation length (e.g. from farm settings). */
    fun dueDate(gestationDays: Int): LocalDate = serviceDate.plusDays(gestationDays.toLong())

    val hasPregnancyCheck: Boolean get() = pregnancyCheckResult != null
}

enum class BreedingEventType { NATURAL, AI, EMBRYO_TRANSFER }

enum class PregnancyCheckResult { PREGNANT, NOT_PREGNANT }

const val DEFAULT_GESTATION_DAYS = 283L
