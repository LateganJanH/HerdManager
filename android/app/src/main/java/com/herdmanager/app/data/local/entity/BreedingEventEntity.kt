package com.herdmanager.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "breeding_events",
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
data class BreedingEventEntity(
    @PrimaryKey
    val id: String,
    val animalId: String,
    val sireIds: List<String> = emptyList(),
    val eventType: String,
    val serviceDate: Long,
    val notes: String? = null,
    val createdAt: Long,
    val pregnancyCheckDateEpochDay: Long? = null,
    val pregnancyCheckResult: String? = null
)
