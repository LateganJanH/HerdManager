package com.herdmanager.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "herds",
    indices = [Index(value = ["farmId", "name"], unique = true)]
)
data class HerdEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val farmId: String,
    val description: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
