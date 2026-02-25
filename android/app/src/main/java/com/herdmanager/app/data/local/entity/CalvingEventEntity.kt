package com.herdmanager.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "calving_events",
    foreignKeys = [
        ForeignKey(
            entity = AnimalEntity::class,
            parentColumns = ["id"],
            childColumns = ["damId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("damId"), Index("breedingEventId")]
)
data class CalvingEventEntity(
    @PrimaryKey
    val id: String,
    val damId: String,
    val calfId: String? = null,
    val breedingEventId: String,
    val actualDate: Long,
    val assistanceRequired: Boolean,
    val calfSex: String? = null,
    val calfWeight: Double? = null,
    val notes: String? = null
)
