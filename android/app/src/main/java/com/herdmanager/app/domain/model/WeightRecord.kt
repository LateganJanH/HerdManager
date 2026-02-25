package com.herdmanager.app.domain.model

import java.time.LocalDate

data class WeightRecord(
    val id: String,
    val animalId: String,
    val date: LocalDate,
    val weightKg: Double,
    val note: String? = null
)
