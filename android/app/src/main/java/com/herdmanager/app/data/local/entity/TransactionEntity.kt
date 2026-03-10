package com.herdmanager.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index("farmId"),
        Index("type"),
        Index("dateEpochDay"),
        Index("animalId")
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val amountCents: Long,
    val dateEpochDay: Long,
    val farmId: String,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val weightKg: Double? = null,
    val pricePerKgCents: Long? = null,
    val animalId: String? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val categoryId: String? = null,
    val description: String? = null
)
