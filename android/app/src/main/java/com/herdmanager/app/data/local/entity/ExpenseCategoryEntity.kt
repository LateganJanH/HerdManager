package com.herdmanager.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expense_categories",
    indices = [Index("farmId")]
)
data class ExpenseCategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val farmId: String,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)
