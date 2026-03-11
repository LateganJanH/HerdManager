package com.herdmanager.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herdmanager.app.domain.model.FarmTask
import com.herdmanager.app.domain.model.TaskStatus
import com.herdmanager.app.domain.repository.AnimalRepository
import com.herdmanager.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class TasksSummary(
    val openCount: Int,
    val dueTodayCount: Int,
    val overdueCount: Int
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val animalRepository: AnimalRepository
) : ViewModel() {

    private val _statusFilter = MutableStateFlow<TaskStatus?>(null)
    val statusFilter: StateFlow<TaskStatus?> = _statusFilter.asStateFlow()

    val allTasks: StateFlow<List<FarmTask>> = taskRepository.observeAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val summary: StateFlow<TasksSummary> = allTasks
        .combine(_statusFilter) { tasks, _ ->
            val today = LocalDate.now()
            val open = tasks.count { it.status != TaskStatus.DONE && it.status != TaskStatus.CANCELLED }
            val dueToday = tasks.count { it.status != TaskStatus.DONE && it.status != TaskStatus.CANCELLED && it.dueDate == today }
            val overdue = tasks.count { it.status != TaskStatus.DONE && it.status != TaskStatus.CANCELLED && it.dueDate != null && it.dueDate!!.isBefore(today) }
            TasksSummary(openCount = open, dueTodayCount = dueToday, overdueCount = overdue)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TasksSummary(0, 0, 0)
        )

    val filteredTasks: StateFlow<List<FarmTask>> = combine(allTasks, _statusFilter) { tasks, filter ->
        if (filter == null) tasks
        else tasks.filter { it.status == filter }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun setStatusFilter(status: TaskStatus?) {
        _statusFilter.value = status
    }

    fun addTask(
        title: String,
        notes: String?,
        dueDate: LocalDate?,
        animalId: String?
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val task = FarmTask(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                notes = notes?.trim()?.takeIf { it.isNotBlank() },
                dueDate = dueDate,
                status = TaskStatus.PENDING,
                animalId = animalId,
                priority = null,
                createdAt = now,
                updatedAt = now
            )
            taskRepository.insert(task)
        }
    }

    fun updateTask(
        id: String,
        title: String,
        notes: String?,
        dueDate: LocalDate?,
        status: TaskStatus,
        animalId: String?
    ) {
        viewModelScope.launch {
            val existing = allTasks.value.find { it.id == id } ?: return@launch
            val now = System.currentTimeMillis()
            val updated = existing.copy(
                title = title.trim(),
                notes = notes?.trim()?.takeIf { it.isNotBlank() },
                dueDate = dueDate,
                status = status,
                animalId = animalId,
                updatedAt = now
            )
            taskRepository.update(updated)
        }
    }

    fun markTaskDone(id: String) {
        viewModelScope.launch {
            taskRepository.updateStatus(id, TaskStatus.DONE)
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch {
            taskRepository.delete(id)
        }
    }

    val animalsForPicker = animalRepository.observeAnimalsByFarm(com.herdmanager.app.domain.model.FarmSettings.DEFAULT_FARM_ID)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}
