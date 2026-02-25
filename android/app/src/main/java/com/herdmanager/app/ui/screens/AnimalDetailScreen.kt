package com.herdmanager.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.herdmanager.app.domain.model.Animal
import com.herdmanager.app.domain.model.BreedingEvent
import com.herdmanager.app.domain.model.Sex
import com.herdmanager.app.domain.model.HealthEvent
import com.herdmanager.app.domain.model.WeightRecord
import com.herdmanager.app.ui.components.AddHealthEventDialog
import com.herdmanager.app.ui.components.LogWeightDialog
import com.herdmanager.app.ui.components.RecordBreedingDialog
import com.herdmanager.app.ui.components.TransferHerdDialog
import com.herdmanager.app.ui.components.PhotosSection
import com.herdmanager.app.ui.components.RecordCalvingDialog
import com.herdmanager.app.ui.components.RecordPregnancyCheckDialog
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalDetailScreen(
    animalId: String,
    onNavigateBack: () -> Unit,
    onEditAnimal: () -> Unit = {},
    onAnimalDeleted: () -> Unit = {},
    viewModel: AnimalDetailViewModel = hiltViewModel()
) {
    val animal by viewModel.animal.collectAsState()
    val herds by viewModel.herds.collectAsState(initial = emptyList())
    val sires by viewModel.sires.collectAsState(initial = emptyList())
    val photos by viewModel.photos.collectAsState(initial = emptyList())
    val breedingEventsWithCalving by viewModel.breedingEventsWithCalving.collectAsState(initial = emptyList())
    val gestationDays by viewModel.gestationDays.collectAsState(initial = com.herdmanager.app.domain.model.FarmSettings.DEFAULT_GESTATION_DAYS)
    val healthEvents by viewModel.healthEvents.collectAsState(initial = emptyList())
    val weightRecords by viewModel.weightRecords.collectAsState(initial = emptyList())
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showTransferHerd by remember { mutableStateOf(false) }
    var showAddHealthEvent by remember { mutableStateOf(false) }
    var showLogWeight by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var hasEmitted by remember { mutableStateOf(false) }
    var hasSeenAnimal by remember { mutableStateOf(false) }
    var hasNavigatedAwayByDelete by remember { mutableStateOf(false) }

    LaunchedEffect(animal) {
        hasEmitted = true
        if (animal != null) hasSeenAnimal = true
        if (animal == null && hasSeenAnimal && !hasNavigatedAwayByDelete) {
            hasNavigatedAwayByDelete = true
            delay(400)
            onNavigateBack()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.operationResult.collect { result ->
            val message = when (result) {
                is AnimalDetailOperationResult.Success -> result.message
                is AnimalDetailOperationResult.Error -> result.message
            }
            snackbarHostState.showSnackbar(message)
            when (result) {
                is AnimalDetailOperationResult.Success -> {
                    when (result.operation) {
                        AnimalDetailOperation.HEALTH -> showAddHealthEvent = false
                        AnimalDetailOperation.WEIGHT -> showLogWeight = false
                        AnimalDetailOperation.TRANSFER -> showTransferHerd = false
                        AnimalDetailOperation.DELETE_ANIMAL -> {
                            delay(1200)
                            onAnimalDeleted()
                        }
                        else -> {}
                    }
                }
                else -> {}
            }
        }
    }

    if (showDeleteConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete animal?") },
            text = { Text("This animal and all associated records will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAnimal()
                    showDeleteConfirm = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(animal?.earTagNumber ?: "Animal") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to herd")
                    }
                },
                actions = {
                    IconButton(onClick = onEditAnimal) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit animal")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete animal")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            animal?.let { a ->
                PhotosSection(
                    photos = photos,
                    animalId = animalId,
                    avatarPhotoId = a.avatarPhotoId,
                    onPhotoAdded = { uri, angle, lat, lng -> viewModel.addPhoto(uri, angle, lat, lng) },
                    onPhotoDeleted = { viewModel.deletePhoto(it) },
                    onSetAvatar = { viewModel.setAvatarPhoto(it.id) }
                )
                Spacer(modifier = Modifier.height(24.dp))
                AnimalInfoSection(
                    animal = a,
                    currentHerdName = a.currentHerdId?.let { hid -> herds.find { it.id == hid }?.name },
                    herds = herds,
                    onTransferClick = { showTransferHerd = true }
                )
                if (a.sex == Sex.FEMALE) {
                    Spacer(modifier = Modifier.height(24.dp))
                    BreedingSection(
                        eventsWithCalving = breedingEventsWithCalving,
                        damEarTag = a.earTagNumber,
                        sires = sires,
                        gestationDays = gestationDays,
                        viewModel = viewModel,
                        onServiceRecorded = {},
                        onCalvingRecorded = {}
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                HealthSection(
                    healthEvents = healthEvents,
                    onAddClick = { showAddHealthEvent = true },
                    onDeleteClick = { viewModel.deleteHealthEvent(it.id) }
                )
                Spacer(modifier = Modifier.height(24.dp))
                WeightsSection(
                    weightRecords = weightRecords,
                    onAddClick = { showLogWeight = true },
                    onDeleteClick = { viewModel.deleteWeightRecord(it.id) }
                )
            } ?: run {
                if (hasEmitted) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Animal not found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack, shape = RoundedCornerShape(12.dp)) {
                            Text("Back to list")
                        }
                    }
                } else {
                    AnimalDetailSkeleton()
                }
            }
        }
    }
    if (showAddHealthEvent) {
        AddHealthEventDialog(
            onConfirm = { eventType, date, product, dosage, withdrawalEnd, notes ->
                viewModel.addHealthEvent(eventType, date, product, dosage, withdrawalEnd, notes)
            },
            onDismiss = { showAddHealthEvent = false }
        )
    }
    if (showTransferHerd) {
        TransferHerdDialog(
            currentHerdId = animal?.currentHerdId,
            currentHerdName = animal?.currentHerdId?.let { hid -> herds.find { it.id == hid }?.name },
            herds = herds,
            onConfirm = { herdId, date, reason ->
                viewModel.transferToHerd(herdId, date, reason)
            },
            onDismiss = { showTransferHerd = false }
        )
    }
    if (showLogWeight) {
        LogWeightDialog(
            initialDate = LocalDate.now(),
            onConfirm = { date, weightKg, note ->
                viewModel.addWeightRecord(date, weightKg, note)
            },
            onDismiss = { showLogWeight = false }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AnimalInfoSection(
    animal: Animal,
    currentHerdName: String? = null,
    herds: List<com.herdmanager.app.domain.model.Herd> = emptyList(),
    onTransferClick: () -> Unit = {}
) {
    Text(
        "Animal details",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(12.dp))
    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoRow("Ear tag", animal.earTagNumber)
            animal.rfid?.let { rfid ->
                InfoRow("RFID", rfid)
            }
            animal.name?.let { name ->
                InfoRow("Name", name)
            }
            InfoRow("Sex", animal.sex.name)
            InfoRow("Breed", animal.breed)
            InfoRow("Date of Birth", animal.dateOfBirth.toString())
            animal.coatColor?.let { color ->
                InfoRow("Coat color", color)
            }
            animal.hornStatus?.let { horn ->
                InfoRow("Horns", horn.name.lowercase().replaceFirstChar { it.uppercase() })
            }
            if (animal.sex == Sex.MALE && animal.isCastrated != null) {
                InfoRow("Castrated", if (animal.isCastrated == true) "Yes" else "No")
            }
            InfoRow("Status", animal.status.name.replace('_', ' '))
            InfoRow("Herd", currentHerdName ?: (animal.currentHerdId?.let { "Unknown" } ?: "Unassigned"))
            if (herds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onTransferClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (currentHerdName != null) "Transfer to different herd" else "Assign to herd")
                }
            }
        }
    }
}

@Composable
private fun BreedingSection(
    eventsWithCalving: List<BreedingEventWithCalving>,
    damEarTag: String,
    sires: List<Animal>,
    gestationDays: Int,
    viewModel: AnimalDetailViewModel,
    onServiceRecorded: () -> Unit = {},
    onCalvingRecorded: () -> Unit = {}
) {
    var showRecordBreeding by remember { mutableStateOf(false) }
    var showCalvingDialogFor by remember { mutableStateOf<BreedingEvent?>(null) }
    var showPregnancyCheckDialogFor by remember { mutableStateOf<BreedingEvent?>(null) }
    LaunchedEffect(Unit) {
        viewModel.operationResult.collect { result ->
            when (result) {
                is AnimalDetailOperationResult.Success -> {
                    when (result.operation) {
                        AnimalDetailOperation.BREEDING -> {
                            showRecordBreeding = false
                            onServiceRecorded()
                        }
                        AnimalDetailOperation.CALVING -> {
                            showCalvingDialogFor = null
                            onCalvingRecorded()
                        }
                        AnimalDetailOperation.PREGNANCY_CHECK -> {
                            showPregnancyCheckDialogFor = null
                        }
                        else -> {}
                    }
                }
                else -> {}
            }
        }
    }
    if (showRecordBreeding) {
        RecordBreedingDialog(
            sires = sires,
            onConfirm = { date, eventType, sireIds, notes ->
                viewModel.recordBreeding(date, eventType, sireIds, notes)
            },
            onDismiss = { showRecordBreeding = false }
        )
    }
    showCalvingDialogFor?.let { event ->
        val calvingsForEvent = eventsWithCalving.find { it.event.id == event.id }?.calvings ?: emptyList()
        RecordCalvingDialog(
            breedingEventId = event.id,
            damEarTag = damEarTag,
            dueDate = event.dueDate(gestationDays),
            existingCalfCount = calvingsForEvent.size,
            onConfirm = { actualDate, assistance, sex, weight, createCalf, calfTag, notes ->
                viewModel.recordCalving(
                    breedingEventId = event.id,
                    actualDate = actualDate,
                    assistanceRequired = assistance,
                    calfSex = sex,
                    calfWeight = weight,
                    createCalf = createCalf,
                    calfEarTag = calfTag,
                    notes = notes
                )
            },
            onDismiss = { showCalvingDialogFor = null }
        )
    }
    showPregnancyCheckDialogFor?.let { event ->
        RecordPregnancyCheckDialog(
            serviceDate = event.serviceDate,
            dueDate = event.dueDate(gestationDays),
            onConfirm = { checkDate, result ->
                viewModel.recordPregnancyCheck(event.id, checkDate, result)
            },
            onDismiss = { showPregnancyCheckDialogFor = null }
        )
    }
    Text(
        "Reproduction",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(12.dp))
    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Button(
                onClick = { showRecordBreeding = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Record service")
            }
            if (eventsWithCalving.isEmpty()) {
                Text(
                    "Record a breeding service to track due dates and pregnancy checks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            for ((event, calvings) in eventsWithCalving) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "Service: ${event.serviceDate} · Due: ${event.dueDate(gestationDays)} · ${event.eventType.name.replace('_', ' ')}" +
                            (event.pregnancyCheckResult?.let { " · Check: ${it.name.replace('_', ' ')}" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    calvings.forEach { calving ->
                        Text(
                            text = "Calf: ${calving.actualDate}" +
                                (calving.calfSex?.let { " · ${it.name.lowercase().replaceFirstChar { c -> c.uppercase() }}" } ?: "") +
                                (calving.calfWeight?.let { " · ${it}kg" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        if (!event.hasPregnancyCheck) {
                            TextButton(onClick = { showPregnancyCheckDialogFor = event }) {
                                Text("Record pregnancy check")
                            }
                        }
                        TextButton(onClick = { showCalvingDialogFor = event }) {
                            Text(if (calvings.isEmpty()) "Record calving" else "Record another calf")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthSection(
    healthEvents: List<HealthEvent>,
    onAddClick: () -> Unit,
    onDeleteClick: (HealthEvent) -> Unit = {}
) {
    var eventToDelete by remember { mutableStateOf<HealthEvent?>(null) }
    eventToDelete?.let { event ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { eventToDelete = null },
            title = { Text("Delete health event?") },
            text = { Text("${event.eventType.name} · ${event.date} will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteClick(event)
                    eventToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { eventToDelete = null }) { Text("Cancel") }
            }
        )
    }
    Text(
        "Health & treatments",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(12.dp))
    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Button(
                onClick = onAddClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Log treatment or vaccination")
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (healthEvents.isEmpty()) {
                Text(
                    "No health events yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            for (event in healthEvents) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${event.eventType.name} · ${event.date}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        event.product?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        event.dosage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        event.withdrawalPeriodEnd?.let { end ->
                            Text("Withdrawal period until $end", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                        event.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    IconButton(onClick = { eventToDelete = event }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightsSection(
    weightRecords: List<WeightRecord>,
    onAddClick: () -> Unit,
    onDeleteClick: (WeightRecord) -> Unit = {}
) {
    var recordToDelete by remember { mutableStateOf<WeightRecord?>(null) }
    recordToDelete?.let { record ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Delete weight record?") },
            text = { Text("${record.date} · ${record.weightKg} kg will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteClick(record)
                    recordToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) { Text("Cancel") }
            }
        )
    }
    Text(
        "Weight & weaning",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(12.dp))
    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Button(
                onClick = onAddClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Log weight")
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (weightRecords.isEmpty()) {
                Text(
                    "No weight records yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            for (record in weightRecords) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${record.date} · ${record.weightKg} kg",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        record.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    IconButton(onClick = { recordToDelete = record }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimalDetailSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            )
            Box(
                modifier = Modifier
                    .height(16.dp)
                    .width(80.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        )
    }
}
