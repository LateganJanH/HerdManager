package com.herdmanager.app.data.repository

import com.herdmanager.app.data.local.dao.FarmTaskDao
import com.herdmanager.app.data.local.entity.FarmTaskEntity
import com.herdmanager.app.domain.model.FarmTask
import com.herdmanager.app.domain.model.TaskStatus
import com.herdmanager.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class TaskRepositoryImpl(
    private val dao: FarmTaskDao
) : TaskRepository {

    override fun observeAllTasks(): Flow<List<FarmTask>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeTasksByStatus(status: TaskStatus): Flow<List<FarmTask>> =
        dao.observeByStatus(status.name).map { list -> list.map { it.toDomain() } }

    override fun observeTasksDueBetween(start: LocalDate, end: LocalDate): Flow<List<FarmTask>> {
        val startEpoch = start.toEpochDay()
        val endEpoch = end.toEpochDay()
        return dao.observeDueBetween(startEpoch, endEpoch).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insert(task: FarmTask) {
        dao.insert(task.toEntity())
    }

    override suspend fun update(task: FarmTask) {
        dao.update(task.toEntity())
    }

    override suspend fun updateStatus(id: String, status: TaskStatus) {
        dao.updateStatus(id, status.name, System.currentTimeMillis())
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }
}

private fun FarmTaskEntity.toDomain(): FarmTask =
    FarmTask(
        id = id,
        title = title,
        notes = notes,
        dueDate = dueDateEpochDay?.let { LocalDate.ofEpochDay(it) },
        status = runCatching { TaskStatus.valueOf(status) }.getOrElse { TaskStatus.PENDING },
        animalId = animalId,
        priority = priority?.let { runCatching { com.herdmanager.app.domain.model.TaskPriority.valueOf(it) }.getOrNull() },
        createdAt = createdAt,
        updatedAt = updatedAt
    )

private fun FarmTask.toEntity(): FarmTaskEntity =
    FarmTaskEntity(
        id = id,
        title = title,
        notes = notes,
        dueDateEpochDay = dueDate?.toEpochDay(),
        status = status.name,
        animalId = animalId,
        priority = priority?.name,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

