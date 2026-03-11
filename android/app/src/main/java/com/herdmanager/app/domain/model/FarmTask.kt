package com.herdmanager.app.domain.model

import java.time.LocalDate

enum class TaskStatus { PENDING, IN_PROGRESS, DONE, CANCELLED }

enum class TaskPriority { LOW, MEDIUM, HIGH }

data class FarmTask(
    val id: String,
    val title: String,
    val notes: String? = null,
    val dueDate: LocalDate? = null,
    val status: TaskStatus = TaskStatus.PENDING,
    val animalId: String? = null,
    val priority: TaskPriority? = null,
    val createdAt: Long,
    val updatedAt: Long
)

