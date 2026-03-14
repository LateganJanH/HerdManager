package com.herdmanager.app.domain.model

import java.time.LocalDate

data class ConditionRecord(
    val id: String,
    val animalId: String,
    val date: LocalDate,
    /** Body condition score, typically 1–9. */
    val score: Int,
    val notes: String? = null
)

