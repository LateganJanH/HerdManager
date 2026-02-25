package com.herdmanager.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "health_events",
    foreignKeys = [
        ForeignKey(
            entity = AnimalEntity::class,
            parentColumns = ["id"],
            childColumns = ["animalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("animalId")]
)
data class HealthEventEntity(
    @PrimaryKey
    val id: String,
    val animalId: String,
    val eventType: String,
    val date: Long,
    val product: String? = null,
    val dosage: String? = null,
    val withdrawalPeriodEnd: Long? = null,
    val notes: String? = null
)
