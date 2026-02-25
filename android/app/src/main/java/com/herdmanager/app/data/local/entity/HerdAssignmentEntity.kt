package com.herdmanager.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "herd_assignments",
    foreignKeys = [
        ForeignKey(
            entity = AnimalEntity::class,
            parentColumns = ["id"],
            childColumns = ["animalId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = HerdEntity::class,
            parentColumns = ["id"],
            childColumns = ["herdId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("animalId"), Index("herdId"), Index(value = ["animalId", "removedAt"])]
)
data class HerdAssignmentEntity(
    @PrimaryKey
    val id: String,
    val animalId: String,
    val herdId: String,
    val assignedAt: Long,
    val removedAt: Long? = null,
    val reason: String? = null
)
