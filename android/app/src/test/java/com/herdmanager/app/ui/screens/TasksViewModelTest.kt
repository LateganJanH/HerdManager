package com.herdmanager.app.ui.screens

import com.herdmanager.app.domain.model.Animal
import com.herdmanager.app.domain.model.FarmTask
import com.herdmanager.app.domain.model.Sex
import com.herdmanager.app.domain.model.TaskStatus
import com.herdmanager.app.domain.repository.AnimalRepository
import com.herdmanager.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.time.LocalDate

private class FakeTaskRepository : TaskRepository {
    private val tasks = mutableMapOf<String, FarmTask>()
    private val allFlow = MutableStateFlow<List<FarmTask>>(emptyList())

    override fun observeAllTasks(): Flow<List<FarmTask>> = allFlow

    override fun observeTasksByStatus(status: TaskStatus): Flow<List<FarmTask>> =
        allFlow.map { list -> list.filter { it.status == status } }

    override fun observeTasksDueBetween(start: LocalDate, end: LocalDate): Flow<List<FarmTask>> =
        allFlow.map { list ->
            list.filter { t ->
                t.dueDate != null && !t.dueDate!!.isBefore(start) && !t.dueDate!!.isAfter(end)
            }
        }

    override suspend fun insert(task: FarmTask) {
        tasks[task.id] = task
        allFlow.value = tasks.values.toList()
    }

    override suspend fun update(task: FarmTask) {
        tasks[task.id] = task
        allFlow.value = tasks.values.toList()
    }

    override suspend fun updateStatus(id: String, status: TaskStatus) {
        tasks[id]?.let { t ->
            tasks[id] = t.copy(status = status, updatedAt = System.currentTimeMillis())
            allFlow.value = tasks.values.toList()
        }
    }

    override suspend fun delete(id: String) {
        tasks.remove(id)
        allFlow.value = tasks.values.toList()
    }

    fun snapshot(): List<FarmTask> = allFlow.value
}

/** Fake for TasksViewModel tests only; distinct name to avoid clash with TransactionsViewModelTest.FakeAnimalRepository. */
private class TasksFakeAnimalRepository : AnimalRepository {
    private val animalsFlow = MutableStateFlow<List<Animal>>(emptyList())

    override fun observeAnimalsByFarm(farmId: String): Flow<List<Animal>> = animalsFlow

    override fun observeAnimalsByFarmAndHerd(farmId: String, herdId: String?): Flow<List<Animal>> =
        animalsFlow

    override suspend fun getAnimalById(id: String): Animal? =
        animalsFlow.value.firstOrNull { it.id == id }

    override suspend fun insertAnimal(animal: Animal) {
        animalsFlow.value = animalsFlow.value + animal
    }

    override suspend fun deleteAnimal(id: String) {
        animalsFlow.value = animalsFlow.value.filterNot { it.id == id }
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TasksDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TasksViewModelTest {

    @get:Rule
    val mainDispatcherRule = TasksDispatcherRule()

    @Test
    fun addTask_insertsTask_allTasksEmitsIt() = runTest {
        val taskRepo = FakeTaskRepository()
        val animalRepo = TasksFakeAnimalRepository()
        val vm = TasksViewModel(taskRepository = taskRepo, animalRepository = animalRepo)

        // Subscribe so stateIn is active
        val job = launch { vm.allTasks.collect {} }
        advanceUntilIdle()

        vm.addTask("New task", null, null, null)
        advanceUntilIdle()

        assertEquals(1, vm.allTasks.value.size)
        assertEquals("New task", vm.allTasks.value.first().title)
        assertEquals(TaskStatus.PENDING, vm.allTasks.value.first().status)
        job.cancel()
    }

    @Test
    fun updateTask_updatesTitleAndStatus() = runTest {
        val taskRepo = FakeTaskRepository()
        val now = System.currentTimeMillis()
        taskRepo.insert(
            FarmTask(
                id = "t1",
                title = "Old",
                notes = null,
                dueDate = null,
                status = TaskStatus.PENDING,
                animalId = null,
                priority = null,
                createdAt = now,
                updatedAt = now
            )
        )

        val animalRepo = TasksFakeAnimalRepository()
        val vm = TasksViewModel(taskRepository = taskRepo, animalRepository = animalRepo)
        val job = launch { vm.allTasks.collect {} }
        advanceUntilIdle()

        vm.updateTask("t1", "Updated title", "notes", null, TaskStatus.IN_PROGRESS, null)
        advanceUntilIdle()

        assertEquals(1, vm.allTasks.value.size)
        assertEquals("Updated title", vm.allTasks.value.first().title)
        assertEquals(TaskStatus.IN_PROGRESS, vm.allTasks.value.first().status)
        job.cancel()
    }

    @Test
    fun setStatusFilter_filtersTasksByStatus() = runTest {
        val taskRepo = FakeTaskRepository()
        val now = System.currentTimeMillis()
        taskRepo.insert(
            FarmTask("1", "P", null, null, TaskStatus.PENDING, null, null, now, now)
        )
        taskRepo.insert(
            FarmTask("2", "D", null, null, TaskStatus.DONE, null, null, now, now)
        )

        val vm = TasksViewModel(taskRepository = taskRepo, animalRepository = TasksFakeAnimalRepository())
        val job = launch { vm.allTasks.collect {} }
        val filterJob = launch { vm.filteredTasks.collect {} }
        advanceUntilIdle()

        assertEquals(2, vm.filteredTasks.value.size)

        vm.setStatusFilter(TaskStatus.PENDING)
        advanceUntilIdle()
        assertEquals(1, vm.filteredTasks.value.size)
        assertEquals("P", vm.filteredTasks.value.first().title)

        vm.setStatusFilter(TaskStatus.DONE)
        advanceUntilIdle()
        assertEquals(1, vm.filteredTasks.value.size)
        assertEquals("D", vm.filteredTasks.value.first().title)

        vm.setStatusFilter(null)
        advanceUntilIdle()
        assertEquals(2, vm.filteredTasks.value.size)

        job.cancel()
        filterJob.cancel()
    }

    @Test
    fun summary_countsOpenDueTodayOverdue() = runTest {
        val taskRepo = FakeTaskRepository()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val now = System.currentTimeMillis()

        taskRepo.insert(
            FarmTask("1", "Open", null, today.plusDays(1), TaskStatus.PENDING, null, null, now, now)
        )
        taskRepo.insert(
            FarmTask("2", "Due today", null, today, TaskStatus.PENDING, null, null, now, now)
        )
        taskRepo.insert(
            FarmTask("3", "Overdue", null, yesterday, TaskStatus.PENDING, null, null, now, now)
        )
        taskRepo.insert(
            FarmTask("4", "Done", null, null, TaskStatus.DONE, null, null, now, now)
        )

        val vm = TasksViewModel(taskRepository = taskRepo, animalRepository = TasksFakeAnimalRepository())
        val job = launch { vm.allTasks.collect {} }
        val summaryJob = launch { vm.summary.collect {} }
        advanceUntilIdle()

        assertEquals(3, vm.summary.value.openCount) // open = not DONE/CANCELLED
        assertEquals(1, vm.summary.value.dueTodayCount)
        assertEquals(1, vm.summary.value.overdueCount)

        job.cancel()
        summaryJob.cancel()
    }
}
