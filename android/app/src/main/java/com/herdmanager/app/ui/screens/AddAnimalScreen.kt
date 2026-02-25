package com.herdmanager.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.herdmanager.app.domain.model.Animal
import com.herdmanager.app.domain.model.AnimalStatus
import com.herdmanager.app.domain.model.Herd
import com.herdmanager.app.domain.model.FarmSettings
import com.herdmanager.app.domain.model.HornStatus
import com.herdmanager.app.domain.model.Sex
import com.herdmanager.app.ui.components.DatePickerDialog
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAnimalScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddAnimalViewModel = hiltViewModel()
) {
    var earTag by rememberSaveable { mutableStateOf("") }
    var rfid by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var breed by rememberSaveable { mutableStateOf("") }
    var sex by rememberSaveable { mutableStateOf(Sex.FEMALE) }
    var isCastrated by rememberSaveable { mutableStateOf(false) }
    var hornStatus by rememberSaveable { mutableStateOf<HornStatus?>(null) }
    var selectedHerdId by rememberSaveable { mutableStateOf<String?>(null) }
    var dateOfBirth by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var coatColor by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val herds by viewModel.herds.collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        viewModel.addResult.collect { result ->
            when (result) {
                is AddAnimalResult.Success -> {
                    // Show snackbar without awaiting (showSnackbar suspends until dismissed)
                    scope.launch { snackbarHostState.showSnackbar("Animal registered") }
                    delay(1200)
                    onNavigateBack()
                }
                is AddAnimalResult.Error -> {
                    error = result.message
                    snackbarHostState.showSnackbar(result.message)
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { dateOfBirth = it; error = null },
            onDismiss = { showDatePicker = false },
            initialDate = dateOfBirth?.atStartOfDay(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: System.currentTimeMillis()
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Register animal") },
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
            val earTagError = error?.takeIf { it.contains("ear tag", ignoreCase = true) }
            OutlinedTextField(
                value = earTag,
                onValueChange = { earTag = it; error = null },
                label = { Text("Ear tag number *") },
                modifier = Modifier.fillMaxWidth().testTag("addAnimal_earTag"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                isError = earTagError != null,
                supportingText = earTagError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = rfid,
                onValueChange = { rfid = it },
                label = { Text("RFID (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            val breedError = error?.takeIf { it.contains("breed", ignoreCase = true) }
            OutlinedTextField(
                value = breed,
                onValueChange = { breed = it; error = null },
                label = { Text("Breed *") },
                modifier = Modifier.fillMaxWidth().testTag("addAnimal_breed"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
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
                modifier = Modifier.fillMaxWidth().testTag("addAnimal_dobButton"),
                shape = RoundedCornerShape(12.dp)
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
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
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
            if (herds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Assign to herd (optional)", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for ((id, label) in listOf(null to "None") + herds.map { h -> h.id to h.name }) {
                        TextButton(
                            onClick = { selectedHerdId = id },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedHerdId == id) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            error?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = msg, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(24.dp))
            val canSave = earTag.isNotBlank() && breed.isNotBlank() && dateOfBirth != null
            Button(
                onClick = {
                    error = null
                    if (canSave) viewModel.addAnimal(
                                Animal(
                                    id = UUID.randomUUID().toString(),
                                    earTagNumber = earTag.trim(),
                                    rfid = rfid.takeIf { it.isNotBlank() },
                                    name = name.takeIf { it.isNotBlank() },
                                    sex = sex,
                                    breed = breed.trim(),
                                    dateOfBirth = dateOfBirth!!,
                                    farmId = FarmSettings.DEFAULT_FARM_ID,
                                    coatColor = coatColor.takeIf { it.isNotBlank() },
                                    hornStatus = hornStatus,
                                    isCastrated = if (sex == Sex.MALE) isCastrated else null,
                                    avatarPhotoId = null,
                                    status = AnimalStatus.ACTIVE
                                ),
                                herdId = selectedHerdId
                            )
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().testTag("addAnimal_save"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        }
    }
}
