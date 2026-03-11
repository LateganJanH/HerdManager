package com.herdmanager.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "farm_tasks",
    indices = [
        Index(value = ["status"]),
        Index(value = ["dueDateEpochDay"]),
        Index(value = ["animalId"])
    ]
)
data class FarmTaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val notes: String? = null,
    /** Optional due date stored as epoch day (UTC). */
    val dueDateEpochDay: Long? = null,
    /** Task status (e.g. PENDING, IN_PROGRESS, DONE, CANCELLED). */
    val status: String,
    /** Optional linked animal ID for animal-specific tasks. */
    val animalId: String? = null,
    val priority: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

