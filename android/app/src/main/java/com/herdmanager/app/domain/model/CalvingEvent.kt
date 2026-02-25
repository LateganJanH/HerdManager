package com.herdmanager.app.domain.model

import java.time.LocalDate

data class CalvingEvent(
    val id: String,
    val damId: String,
    val calfId: String? = null,
    val breedingEventId: String,
    val actualDate: LocalDate,
    val assistanceRequired: Boolean,
    val calfSex: Sex? = null,
    val calfWeight: Double? = null,
    val notes: String? = null
)
