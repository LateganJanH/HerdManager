package com.herdmanager.app.data.repository

import com.herdmanager.app.data.local.dao.FarmTaskDao
import com.herdmanager.app.data.local.entity.FarmTaskEntity
import com.herdmanager.app.domain.model.FarmTask
import com.herdmanager.app.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private class FakeFarmTaskDao : FarmTaskDao {
    private val entities = mutableMapOf<String, FarmTaskEntity>()
    private val allFlow = MutableStateFlow<List<FarmTaskEntity>>(emptyList())

    private fun emitAll() {
        allFlow.value = entities.values.toList()
    }

    override fun observeAll(): Flow<List<FarmTaskEntity>> = allFlow

    override fun observeByStatus(status: String): Flow<List<FarmTaskEntity>> =
        allFlow.map { list -> list.filter { it.status == status } }

    override fun observeDueBetween(startEpochDay: Long, endEpochDay: Long): Flow<List<FarmTaskEntity>> =
        allFlow.map { list ->
            list.filter { e ->
                e.dueDateEpochDay != null &&
                    e.dueDateEpochDay >= startEpochDay &&
                    e.dueDateEpochDay <= endEpochDay
            }
        }

    override suspend fun insert(task: FarmTaskEntity) {
        entities[task.id] = task
        emitAll()
    }

    override suspend fun update(task: FarmTaskEntity) {
        entities[task.id] = task
        emitAll()
    }

    override suspend fun updateStatus(id: String, status: String, updatedAt: Long) {
        entities[id]?.let { e ->
            entities[id] = e.copy(status = status, updatedAt = updatedAt)
            emitAll()
        }
    }

    override suspend fun deleteById(id: String) {
        entities.remove(id)
        emitAll()
    }

    override suspend fun getAll(): List<FarmTaskEntity> = entities.values.toList()

    override suspend fun getById(id: String): FarmTaskEntity? = entities[id]

    override suspend fun insertAll(tasks: List<FarmTaskEntity>) {
        tasks.forEach { entities[it.id] = it }
        emitAll()
    }

    override suspend fun deleteAll() {
        entities.clear()
        emitAll()
    }
}

class TaskRepositoryImplTest {

    private fun task(
        id: String = "t1",
        title: String = "Task 1",
        notes: String? = null,
        status: TaskStatus = TaskStatus.PENDING,
        dueDate: LocalDate? = null,
        createdAt: Long = 1L,
        updatedAt: Long = 1L
    ) = FarmTask(
        id = id,
        title = title,
        notes = notes,
        dueDate = dueDate,
        status = status,
        animalId = null,
        priority = null,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    @Test
    fun insertTask_preservesFields_observeAllEmitsIt() = runBlocking {
        val dao = FakeFarmTaskDao()
        val repo = TaskRepositoryImpl(dao)

        val t = task(id = "t1", title = "Vet check", notes = "Annual", dueDate = LocalDate.of(2026, 3, 15))
        repo.insert(t)

        val list = repo.observeAllTasks().first()
        assertEquals(1, list.size)
        assertEquals("t1", list[0].id)
        assertEquals("Vet check", list[0].title)
        assertEquals(TaskStatus.PENDING, list[0].status)
        assertEquals(LocalDate.of(2026, 3, 15), list[0].dueDate)
    }

    @Test
    fun observeAll_emitsAllTasksInOrder() = runBlocking {
        val dao = FakeFarmTaskDao()
        val repo = TaskRepositoryImpl(dao)

        repo.insert(task("a", "A", notes = null, createdAt = 1, updatedAt = 1))
        repo.insert(task("b", "B", notes = null, createdAt = 2, updatedAt = 2))

        val list = repo.observeAllTasks().first()
        assertEquals(2, list.size)
        assertTrue(list.any { it.id == "a" })
        assertTrue(list.any { it.id == "b" })
    }

    @Test
    fun observeTasksByStatus_filtersByStatus() = runBlocking {
        val dao = FakeFarmTaskDao()
        val repo = TaskRepositoryImpl(dao)

        repo.insert(task("1", "One", notes = null, status = TaskStatus.PENDING))
        repo.insert(task("2", "Two", notes = null, status = TaskStatus.IN_PROGRESS))
        repo.insert(task("3", "Three", notes = null, status = TaskStatus.DONE))

        val pending = repo.observeTasksByStatus(TaskStatus.PENDING).first()
        assertEquals(1, pending.size)
        assertEquals("1", pending[0].id)

        val done = repo.observeTasksByStatus(TaskStatus.DONE).first()
        assertEquals(1, done.size)
        assertEquals("3", done[0].id)
    }

    @Test
    fun updateStatus_markDone_updatesStatus() = runBlocking {
        val dao = FakeFarmTaskDao()
        val repo = TaskRepositoryImpl(dao)

        repo.insert(task("t1", "Do it", notes = null, status = TaskStatus.PENDING))
        repo.updateStatus("t1", TaskStatus.DONE)

        val list = repo.observeAllTasks().first()
        assertEquals(1, list.size)
        assertEquals(TaskStatus.DONE, list[0].status)
    }

    @Test
    fun delete_removesTask() = runBlocking {
        val dao = FakeFarmTaskDao()
        val repo = TaskRepositoryImpl(dao)

        repo.insert(task("t1", "Delete me", notes = null))
        var list = repo.observeAllTasks().first()
        assertEquals(1, list.size)

        repo.delete("t1")
        list = repo.observeAllTasks().first()
        assertEquals(0, list.size)
    }

    @Test
    fun observeTasksDueBetween_returnsTasksInDateRange() = runBlocking {
        val dao = FakeFarmTaskDao()
        val repo = TaskRepositoryImpl(dao)

        val start = LocalDate.of(2026, 3, 1)
        val end = LocalDate.of(2026, 3, 31)
        repo.insert(task("in", "In range", notes = null, dueDate = LocalDate.of(2026, 3, 15)))
        repo.insert(task("out", "Out range", notes = null, dueDate = LocalDate.of(2026, 4, 10)))

        val inRange = repo.observeTasksDueBetween(start, end).first()
        assertEquals(1, inRange.size)
        assertEquals("in", inRange[0].id)
    }
}
