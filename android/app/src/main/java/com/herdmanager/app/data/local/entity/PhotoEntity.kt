package com.herdmanager.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photos",
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
data class PhotoEntity(
    @PrimaryKey
    val id: String,
    val animalId: String,
    val angle: String,
    val uri: String,
    val capturedAt: Long,
    val latitude: Double? = null,
    val longitude: Double? = null
)
