package com.herdmanager.app.domain.model

import java.time.LocalDate

/**
 * Tracks an animal's assignment to a herd over time.
 * removedAt null = currently assigned to this herd.
 */
data class HerdAssignment(
    val id: String,
    val animalId: String,
    val herdId: String,
    val assignedAt: LocalDate,
    val removedAt: LocalDate? = null,
    val reason: String? = null
) {
    val isCurrent: Boolean get() = removedAt == null
}
