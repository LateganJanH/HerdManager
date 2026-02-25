package com.herdmanager.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.herdmanager.app.domain.model.Animal
import com.herdmanager.app.domain.model.AnimalStatus
import com.herdmanager.app.domain.model.Herd
import com.herdmanager.app.domain.model.HornStatus
import com.herdmanager.app.domain.model.Sex
import com.herdmanager.app.ui.components.DatePickerDialog
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAnimalScreen(
    animalId: String,
    onNavigateBack: () -> Unit,
    viewModel: EditAnimalViewModel = hiltViewModel()
) {
    val animal by viewModel.animal.collectAsState()
    val animalLoaded by viewModel.animalLoaded.collectAsState()
    var earTag by remember { mutableStateOf("") }
    var rfid by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf(Sex.FEMALE) }
    var dateOfBirth by remember { mutableStateOf<LocalDate?>(null) }
    var coatColor by remember { mutableStateOf("") }
    var hornStatus by remember { mutableStateOf<HornStatus?>(null) }
    var isCastrated by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(AnimalStatus.ACTIVE) }
    var showDatePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.updateResult.collect { result ->
            when (result) {
                is UpdateAnimalResult.Success -> {
                    snackbarHostState.showSnackbar("Changes saved")
                    delay(1200)
                    onNavigateBack()
                }
                is UpdateAnimalResult.Error -> {
                    error = result.message
                    snackbarHostState.showSnackbar(result.message)
                }
            }
        }
    }

    val herds by viewModel.herds.collectAsState(initial = emptyList())
    val selectedHerdId by viewModel.selectedHerdId.collectAsState()

    LaunchedEffect(animal) {
        animal?.let { a ->
            earTag = a.earTagNumber
            rfid = a.rfid ?: ""
            name = a.name ?: ""
            breed = a.breed
            sex = a.sex
            dateOfBirth = a.dateOfBirth
            coatColor = a.coatColor ?: ""
            hornStatus = a.hornStatus
            isCastrated = a.isCastrated == true
            status = a.status
            viewModel.setSelectedHerd(a.currentHerdId)
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { dateOfBirth = it; error = null },
            onDismiss = { showDatePicker = false },
            initialDate = dateOfBirth?.atStartOfDay(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                ?: System.currentTimeMillis()
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit Animal") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            when {
                !animalLoaded -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    }
                }
                animal == null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Animal not found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) { Text("Back to list") }
                    }
                }
                else -> {
            val earTagError = error?.takeIf { it.contains("ear tag", ignoreCase = true) }
            OutlinedTextField(
                value = earTag,
                onValueChange = { earTag = it; error = null },
                label = { Text("Ear tag number *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = earTagError != null,
                supportingText = earTagError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = rfid,
                onValueChange = { rfid = it },
                label = { Text("RFID (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            val breedError = error?.takeIf { it.contains("breed", ignoreCase = true) }
            OutlinedTextField(
                value = breed,
                onValueChange = { breed = it; error = null },
                label = { Text("Breed *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = breedError != null,
                supportingText = breedError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("Sex *", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Sex.entries.forEach { s ->
                    TextButton(
                        onClick = {
                            sex = s
                            if (s == Sex.FEMALE) isCastrated = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = s.name,
                            color = if (sex == s) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            if (sex == Sex.MALE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isCastrated,
                        onCheckedChange = { isCastrated = it }
                    )
                    Text("Castrated (steer)", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            val dobError = error?.takeIf { it.contains("date", ignoreCase = true) }
            Button(
                onClick = { showDatePicker = true; error = null },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Date of Birth: ${dateOfBirth?.toString() ?: "Select"}")
            }
            if (dobError != null) {
                Text(text = dobError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = coatColor,
                onValueChange = { coatColor = it },
                label = { Text("Coat Color (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (herds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Herd", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { viewModel.setSelectedHerd(null) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "None",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedHerdId == null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    for (herd in herds) {
                        TextButton(
                            onClick = { viewModel.setSelectedHerd(herd.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = herd.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedHerdId == herd.id) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Horn status (optional)", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (listOf(null) + HornStatus.entries).forEach { h ->
                    val label = h?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Not set"
                    TextButton(
                        onClick = { hornStatus = h },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hornStatus == h) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Status", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AnimalStatus.entries.forEach { s ->
                    TextButton(
                        onClick = { status = s },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = s.name.replace('_', ' '),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (status == s) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            error?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = msg, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(24.dp))
            val canSave = earTag.isNotBlank() && breed.isNotBlank() && dateOfBirth != null && animal != null
            Button(
                onClick = {
                    error = null
                    if (canSave) viewModel.updateAnimal(
                        animal!!.copy(
                            earTagNumber = earTag.trim(),
                            rfid = rfid.takeIf { it.isNotBlank() },
                            name = name.takeIf { it.isNotBlank() },
                            sex = sex,
                            breed = breed.trim(),
                            dateOfBirth = dateOfBirth!!,
                            coatColor = coatColor.takeIf { it.isNotBlank() },
                            hornStatus = hornStatus,
                            isCastrated = if (sex == Sex.MALE) isCastrated else null,
                            avatarPhotoId = animal!!.avatarPhotoId,
                            status = status
                        ),
                        newHerdId = selectedHerdId
                    )
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
            }
            }
        }
    }
}
