package com.herdmanager.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.herdmanager.app.domain.model.Animal
import com.herdmanager.app.domain.model.BreedingEventType
import java.time.LocalDate

@Composable
fun RecordBreedingDialog(
    sires: List<Animal>,
    onConfirm: (LocalDate, BreedingEventType, List<String>, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var serviceDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var eventType by remember { mutableStateOf(BreedingEventType.NATURAL) }
    var selectedSireIds by remember { mutableStateOf(setOf<String>()) }
    var notes by remember { mutableStateOf("") }

    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { serviceDate = it },
            onDismiss = { showDatePicker = false },
            initialDate = serviceDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record breeding service") },
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { showDatePicker = true }) {
                        Text("Service date: $serviceDate")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Event type", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    BreedingEventType.entries.forEach { type ->
                        TextButton(
                            onClick = { eventType = type },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = type.name.replace('_', ' '),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (eventType == type) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Candidate sires (optional, multiple possible)", style = MaterialTheme.typography.bodyMedium)
                if (sires.isEmpty()) {
                    Text(
                        "No male animals in herd. Add or assign bulls to link to service.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(sires) { sire ->
                        FilterChip(
                            selected = selectedSireIds.contains(sire.id),
                            onClick = {
                                selectedSireIds = if (selectedSireIds.contains(sire.id)) {
                                    selectedSireIds - sire.id
                                } else {
                                    selectedSireIds + sire.id
                                }
                            },
                            label = { Text(sire.earTagNumber + (sire.name?.let { " ($it)" } ?: "")) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(serviceDate, eventType, selectedSireIds.toList(), notes.takeIf { it.isNotBlank() })
                }
            ) {
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
