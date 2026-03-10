package com.herdmanager.app.domain.model

/** Category for expense transactions (e.g. Feed, Vet, Labour). */
data class ExpenseCategory(
    val id: String,
    val name: String,
    val farmId: String,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)
