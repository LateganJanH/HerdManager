package com.herdmanager.app.domain.model

import java.time.LocalDate

data class HealthEvent(
    val id: String,
    val animalId: String,
    val eventType: HealthEventType,
    val date: LocalDate,
    val product: String? = null,
    val dosage: String? = null,
    val withdrawalPeriodEnd: LocalDate? = null,
    val notes: String? = null
)

enum class HealthEventType {
    VACCINATION,
    TREATMENT,
    DISEASE,
    WITHDRAWAL,
    CASTRATION
}
