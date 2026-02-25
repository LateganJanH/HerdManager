package com.herdmanager.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "animals",
    indices = [Index(value = ["earTagNumber", "farmId"], unique = true)]
)
data class AnimalEntity(
    @PrimaryKey
    val id: String,
    val earTagNumber: String,
    val rfid: String? = null,
    val name: String? = null,
    val sex: String,
    val breed: String,
    val dateOfBirth: Long,
    val farmId: String,
    val currentHerdId: String? = null,
    val coatColor: String? = null,
    val hornStatus: String? = null,
    val isCastrated: Boolean? = null,
    val avatarPhotoId: String? = null,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: String = "PENDING"
)
