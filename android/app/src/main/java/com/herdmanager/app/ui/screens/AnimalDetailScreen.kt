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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.herdmanager.app.domain.model.Animal
import com.herdmanager.app.domain.model.BreedingEvent
import com.herdmanager.app.domain.model.Sex
import com.herdmanager.app.domain.model.HealthEvent
import com.herdmanager.app.domain.model.WeightRecord
import com.herdmanager.app.domain.model.ConditionRecord
import com.herdmanager.app.ui.components.AddHealthEventDialog
import com.herdmanager.app.ui.components.LogWeightDialog
import com.herdmanager.app.ui.components.RecordBreedingDialog
import com.herdmanager.app.ui.components.TransferHerdDialog
import com.herdmanager.app.ui.components.PhotosSection
import com.herdmanager.app.ui.components.RecordCalvingDialog
import com.herdmanager.app.ui.components.RecordPregnancyCheckDialog
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
    val sireAndDam by viewModel.sireAndDam.collectAsState(initial = Pair(null as Animal?, null as Animal?))
    val photos by viewModel.photos.collectAsState(initial = emptyList())
    val breedingEventsWithCalving by viewModel.breedingEventsWithCalving.collectAsState(initial = emptyList())
    val gestationDays by viewModel.gestationDays.collectAsState(initial = com.herdmanager.app.domain.model.FarmSettings.DEFAULT_GESTATION_DAYS)
    val healthEvents by viewModel.healthEvents.collectAsState(initial = emptyList())
    val weightRecords by viewModel.weightRecords.collectAsState(initial = emptyList())
    val growthSummary by viewModel.growthSummary.collectAsState(initial = null)
    val conditionRecords by viewModel.conditionRecords.collectAsState(initial = emptyList())
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showTransferHerd by remember { mutableStateOf(false) }
    var showAddHealthEvent by remember { mutableStateOf(false) }
    var showLogWeight by remember { mutableStateOf(false) }
    var healthEventToEdit by remember { mutableStateOf<HealthEvent?>(null) }
    var weightRecordToEdit by remember { mutableStateOf<WeightRecord?>(null) }
    var conditionRecordToEdit by remember { mutableStateOf<ConditionRecord?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = remember {
        context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    }
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
                                AnimalDetailOperation.WEIGHT -> {
                                    showLogWeight = false
                                    weightRecordToEdit = null
                                }
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
                    onSetAvatar = { viewModel.setAvatarPhoto(it.id) },
                    onTextDetected = { text ->
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Detected text: $text",
                                actionLabel = "Copy",
                                duration = SnackbarDuration.Long,
                                withDismissAction = true
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Detected text", text))
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
                AnimalInfoSection(
                    animal = a,
                    sire = sireAndDam.first,
                    dam = sireAndDam.second,
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
                    onEditClick = { healthEventToEdit = it },
                    onDeleteClick = { viewModel.deleteHealthEvent(it.id) }
                )
                Spacer(modifier = Modifier.height(24.dp))
                ConditionSection(
                    records = conditionRecords,
                    onAddClick = { conditionRecordToEdit = ConditionRecord(
                        id = "",
                        animalId = animalId,
                        date = LocalDate.now(),
                        score = 5,
                        notes = null
                    ) },
                    onEditClick = { conditionRecordToEdit = it },
                    onDeleteClick = { viewModel.deleteConditionRecord(it.id) }
                )
                Spacer(modifier = Modifier.height(24.dp))
                WeightsSection(
                    weightRecords = weightRecords,
                    growthSummary = growthSummary,
                    onAddClick = { showLogWeight = true },
                    onEditClick = { weightRecordToEdit = it },
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
    if (showAddHealthEvent || healthEventToEdit != null) {
        AddHealthEventDialog(
            onConfirm = { eventId, eventType, date, product, dosage, withdrawalEnd, notes ->
                val aid = animal?.id
                if (eventId != null && aid != null) {
                    viewModel.updateHealthEvent(
                        HealthEvent(
                            id = eventId,
                            animalId = aid,
                            eventType = eventType,
                            date = date,
                            product = product,
                            dosage = dosage,
                            withdrawalPeriodEnd = withdrawalEnd,
                            notes = notes
                        )
                    )
                } else {
                    viewModel.addHealthEvent(eventType, date, product, dosage, withdrawalEnd, notes)
                }
                showAddHealthEvent = false
                healthEventToEdit = null
            },
            onDismiss = {
                showAddHealthEvent = false
                healthEventToEdit = null
            },
            existing = healthEventToEdit
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
    if (showLogWeight || weightRecordToEdit != null) {
        LogWeightDialog(
            initialDate = weightRecordToEdit?.date ?: LocalDate.now(),
            onConfirm = { recordId, date, weightKg, note ->
                val aid = animal?.id
                if (recordId != null && aid != null) {
                    viewModel.updateWeightRecord(
                        WeightRecord(
                            id = recordId,
                            animalId = aid,
                            date = date,
                            weightKg = weightKg,
                            note = note
                        )
                    )
                } else {
                    viewModel.addWeightRecord(date, weightKg, note)
                }
                showLogWeight = false
                weightRecordToEdit = null
            },
            onDismiss = {
                showLogWeight = false
                weightRecordToEdit = null
            },
            existing = weightRecordToEdit
        )
    }
    if (conditionRecordToEdit != null) {
        ConditionRecordDialog(
            existing = conditionRecordToEdit,
            onConfirm = { recordId, date, score, notes ->
                val aid = animal?.id ?: return@ConditionRecordDialog
                if (recordId.isNullOrEmpty()) {
                    viewModel.addConditionRecord(date, score, notes)
                } else {
                    viewModel.updateConditionRecord(
                        ConditionRecord(
                            id = recordId,
                            animalId = aid,
                            date = date,
                            score = score,
                            notes = notes
                        )
                    )
                }
                conditionRecordToEdit = null
            },
            onDismiss = { conditionRecordToEdit = null }
        )
    }
}

@Composable
private fun ConditionSection(
    records: List<ConditionRecord>,
    onAddClick: () -> Unit,
    onEditClick: (ConditionRecord) -> Unit,
    onDeleteClick: (ConditionRecord) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Condition", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            TextButton(onClick = onAddClick) {
                Text("Record score")
            }
        }
        if (records.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "No condition scores yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            records.forEach { record ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Score ${record.score}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            record.date.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        record.notes?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { onEditClick(record) }) {
                            Text("Edit")
                        }
                        TextButton(onClick = { onDeleteClick(record) }) {
                            Text(
                                "Delete",
                                color = MaterialTheme.colorScheme.error
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
private fun ConditionRecordDialog(
    existing: ConditionRecord?,
    onConfirm: (recordId: String?, date: LocalDate, score: Int, notes: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var date by remember(existing) { mutableStateOf(existing?.date ?: LocalDate.now()) }
    var score by remember(existing) { mutableStateOf(existing?.score ?: 5) }
    var notes by remember(existing) { mutableStateOf(existing?.notes ?: "") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Record condition score" else "Edit condition score") },
        text = {
            Column {
                Text("Date: $date", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Score (1–9)", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    (1..9).forEach { value ->
                        Button(
                            onClick = { score = value },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(width = 40.dp, height = 36.dp),
                            enabled = true
                        ) {
                            Text(
                                value.toString(),
                                color = if (score == value) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(existing?.id, date, score, notes.takeIf { it.isNotBlank() }) }) {
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
    sire: Animal? = null,
    dam: Animal? = null,
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
            sire?.let { InfoRow("Sire", it.earTagNumber + (it.name?.let { n -> " ($n)" } ?: "")) }
            dam?.let { InfoRow("Dam", it.earTagNumber + (it.name?.let { n -> " ($n)" } ?: "")) }
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
    onEditClick: (HealthEvent) -> Unit = {},
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
                    Row {
                        IconButton(onClick = { onEditClick(event) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { eventToDelete = event }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightsSection(
    weightRecords: List<WeightRecord>,
    growthSummary: GrowthSummary?,
    onAddClick: () -> Unit,
    onEditClick: (WeightRecord) -> Unit = {},
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
            // Growth summary card
            val latest = weightRecords.maxByOrNull { it.date }
            if (latest != null) {
                val daysSinceLatest = ChronoUnit.DAYS.between(latest.date, LocalDate.now()).coerceAtLeast(0)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "Latest: ${"%.1f".format(latest.weightKg)} kg (${latest.date})" +
                            if (daysSinceLatest > 0) " · $daysSinceLatest days ago" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    growthSummary?.let { g ->
                        Text(
                            text = "Average gain: ${"%.2f".format(g.gainPerDayKg)} kg/day over ${g.daysBetween} days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${record.date} · ${record.weightKg} kg",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        record.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    Row {
                        IconButton(onClick = { onEditClick(record) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit weight")
                        }
                        IconButton(onClick = { recordToDelete = record }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete weight")
                        }
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
