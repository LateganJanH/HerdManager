package com.herdmanager.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.herdmanager.app.domain.model.FarmTask
import com.herdmanager.app.domain.model.TaskStatus
import com.herdmanager.app.ui.components.HorizontalFilterChips
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onNavigateBack: () -> Unit,
    onAnimalClick: (String) -> Unit = {},
    viewModel: TasksViewModel = hiltViewModel()
) {
    val tasks by viewModel.filteredTasks.collectAsState(initial = emptyList())
    val summary by viewModel.summary.collectAsState(initial = TasksSummary(0, 0, 0))
    val statusFilter by viewModel.statusFilter.collectAsState(initial = null)
    val animals by viewModel.animalsForPicker.collectAsState(initial = emptyList())
    val earTagByAnimalId = remember(animals) { animals.associate { it.id to it.earTagNumber } }

    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<FarmTask?>(null) }
    var taskToDelete by remember { mutableStateOf<FarmTask?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks & reminders") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("tasks_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add task")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("tasks_screen")
        ) {
            // Summary row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Open: ${summary.openCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Due today: ${summary.dueTodayCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Overdue: ${summary.overdueCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (summary.overdueCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Status filter chips
            HorizontalFilterChips(
                options = listOf(
                    "All" to (statusFilter == null),
                    "Pending" to (statusFilter == TaskStatus.PENDING),
                    "In progress" to (statusFilter == TaskStatus.IN_PROGRESS),
                    "Done" to (statusFilter == TaskStatus.DONE)
                ),
                onOptionSelected = { index ->
                    viewModel.setStatusFilter(
                        when (index) {
                            0 -> null
                            1 -> TaskStatus.PENDING
                            2 -> TaskStatus.IN_PROGRESS
                            3 -> TaskStatus.DONE
                            else -> null
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (tasks.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No tasks yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tap + to add a task or reminder.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            earTag = task.animalId?.let { earTagByAnimalId[it] },
                            onTap = { taskToEdit = task },
                            onLongClick = { taskToDelete = task },
                            onMarkDone = { viewModel.markTaskDone(task.id) },
                            onAnimalClick = task.animalId?.let { { onAnimalClick(it) } }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditTaskDialog(
            task = null,
            animals = animals,
            onDismiss = { showAddDialog = false },
            onSave = { title, notes, dueDate, animalId, _ ->
                viewModel.addTask(title, notes, dueDate, animalId)
                showAddDialog = false
            }
        )
    }

    taskToEdit?.let { task ->
        AddEditTaskDialog(
            task = task,
            animals = animals,
            onDismiss = { taskToEdit = null },
            onSave = { title, notes, dueDate, animalId, status ->
                viewModel.updateTask(task.id, title, notes, dueDate, status, animalId)
                taskToEdit = null
            }
        )
    }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete task") },
            text = { Text("Delete \"${task.title}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTask(task.id)
                    taskToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskRow(
    task: FarmTask,
    earTag: String?,
    onTap: () -> Unit,
    onLongClick: () -> Unit,
    onMarkDone: () -> Unit,
    onAnimalClick: (() -> Unit)?
) {
    val today = LocalDate.now()
    val isOverdue = task.dueDate != null && task.dueDate!!.isBefore(today) && task.status != TaskStatus.DONE
    val isDueToday = task.dueDate == today

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (task.status != TaskStatus.DONE && task.status != TaskStatus.CANCELLED) {
                Checkbox(
                    checked = false,
                    onCheckedChange = { if (it) onMarkDone() },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                task.dueDate?.let { due ->
                    Text(
                        DateTimeFormatter.ISO_LOCAL_DATE.format(due) + when {
                            isOverdue -> " (overdue)"
                            isDueToday -> " (today)"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isOverdue -> MaterialTheme.colorScheme.error
                            isDueToday -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        task.status.name.replace('_', ' ').lowercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (earTag != null) {
                        if (onAnimalClick != null) {
                            Text(
                                "· $earTag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable(onClick = onAnimalClick)
                            )
                        } else {
                            Text(
                                "· $earTag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditTaskDialog(
    task: FarmTask?,
    animals: List<com.herdmanager.app.domain.model.Animal>,
    onDismiss: () -> Unit,
    onSave: (title: String, notes: String?, LocalDate?, animalId: String?, TaskStatus) -> Unit
) {
    var title by remember(task) { mutableStateOf(task?.title ?: "") }
    var notes by remember(task) { mutableStateOf(task?.notes ?: "") }
    var dueDate by remember(task) { mutableStateOf(task?.dueDate ?: LocalDate.now()) }
    var selectedAnimalId by remember(task) { mutableStateOf(task?.animalId) }
    var selectedStatus by remember(task) { mutableStateOf(task?.status ?: TaskStatus.PENDING) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (task == null) "Add task" else "Edit task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                OutlinedTextField(
                    value = DateTimeFormatter.ISO_LOCAL_DATE.format(dueDate),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Due date") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    dueDate = LocalDate.of(year, month + 1, dayOfMonth)
                                },
                                dueDate.year,
                                dueDate.monthValue - 1,
                                dueDate.dayOfMonth
                            ).show()
                        }
                )
                // Animal link picker
                var showAnimalPicker by remember { mutableStateOf(false) }
                val selectedAnimal = animals.find { it.id == selectedAnimalId }
                OutlinedTextField(
                    value = selectedAnimal?.earTagNumber ?: "None",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Link to animal (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAnimalPicker = true }
                )
                if (showAnimalPicker) {
                    AlertDialog(
                        onDismissRequest = { showAnimalPicker = false },
                        title = { Text("Select animal") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = {
                                    selectedAnimalId = null
                                    showAnimalPicker = false
                                }) { Text("None") }
                                animals.forEach { animal ->
                                    TextButton(
                                        onClick = {
                                            selectedAnimalId = animal.id
                                            showAnimalPicker = false
                                        }
                                    ) {
                                        Text("${animal.earTagNumber}${animal.name?.let { " – $it" } ?: ""}")
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showAnimalPicker = false }) { Text("Cancel") }
                        }
                    )
                }
            }
            if (task != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Status", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(TaskStatus.PENDING, TaskStatus.IN_PROGRESS, TaskStatus.DONE).forEach { status ->
                        val selected = selectedStatus == status
                        TextButton(
                            onClick = { selectedStatus = status },
                            colors = if (selected) androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ) else androidx.compose.material3.ButtonDefaults.textButtonColors()
                        ) {
                            Text(status.name.replace('_', ' ').lowercase())
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) {
                    onSave(
                        title.trim(),
                        notes.trim().takeIf { it.isNotBlank() },
                        dueDate,
                        selectedAnimalId,
                        selectedStatus
                    )
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
