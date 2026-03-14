package com.herdmanager.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "condition_records",
    foreignKeys = [
        ForeignKey(
            entity = AnimalEntity::class,
            parentColumns = ["id"],
            childColumns = ["animalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("animalId"),
        Index("dateEpochDay")
    ]
)
data class ConditionRecordEntity(
    @PrimaryKey
    val id: String,
    val animalId: String,
    /** Epoch day (LocalDate.toEpochDay()). */
    val dateEpochDay: Long,
    /** Body condition score, e.g. 1–9. */
    val score: Int,
    val notes: String? = null,
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = 0L
)

