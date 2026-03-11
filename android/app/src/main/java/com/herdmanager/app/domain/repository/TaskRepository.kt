package com.herdmanager.app.domain.repository

import com.herdmanager.app.domain.model.FarmTask
import com.herdmanager.app.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TaskRepository {
    fun observeAllTasks(): Flow<List<FarmTask>>
    fun observeTasksByStatus(status: TaskStatus): Flow<List<FarmTask>>
    fun observeTasksDueBetween(start: LocalDate, end: LocalDate): Flow<List<FarmTask>>
    suspend fun insert(task: FarmTask)
    suspend fun update(task: FarmTask)
    suspend fun updateStatus(id: String, status: TaskStatus)
    suspend fun delete(id: String)
}

