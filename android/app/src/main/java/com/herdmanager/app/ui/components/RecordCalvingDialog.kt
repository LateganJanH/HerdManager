package com.herdmanager.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.herdmanager.app.domain.model.Sex
import com.herdmanager.app.ui.components.DatePickerDialog
import java.time.LocalDate

@Composable
fun RecordCalvingDialog(
    breedingEventId: String,
    damEarTag: String,
    dueDate: LocalDate,
    existingCalfCount: Int = 0,
    onConfirm: (
        actualDate: LocalDate,
        assistanceRequired: Boolean,
        calfSex: Sex?,
        calfWeight: Double?,
        createCalf: Boolean,
        calfEarTag: String,
        notes: String?
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var actualDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var assistanceRequired by remember { mutableStateOf(false) }
    var calfSex by remember { mutableStateOf<Sex?>(null) }
    var calfWeightStr by remember { mutableStateOf("") }
    var createCalf by remember { mutableStateOf(true) }
    var calfEarTag by remember(existingCalfCount) { mutableStateOf("$damEarTag-C${existingCalfCount + 1}") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { actualDate = it },
            onDismiss = { showDatePicker = false },
            initialDate = actualDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Calving") },
        text = {
            Column {
                Text("Dam: $damEarTag · Due: $dueDate")
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { showDatePicker = true }) {
                        Text("Actual date: $actualDate")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Assistance required?")
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(selected = !assistanceRequired, onClick = { assistanceRequired = false })
                    Text("No")
                    RadioButton(selected = assistanceRequired, onClick = { assistanceRequired = true })
                    Text("Yes")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Calf sex")
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(selected = calfSex == null, onClick = { calfSex = null })
                    Text("Unknown")
                    RadioButton(selected = calfSex == Sex.MALE, onClick = { calfSex = Sex.MALE })
                    Text("Male")
                    RadioButton(selected = calfSex == Sex.FEMALE, onClick = { calfSex = Sex.FEMALE })
                    Text("Female")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = calfWeightStr,
                    onValueChange = { calfWeightStr = it },
                    label = { Text("Calf weight (kg)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(selected = createCalf, onClick = { createCalf = true })
                    Text("Create calf record")
                }
                if (createCalf) {
                    OutlinedTextField(
                        value = calfEarTag,
                        onValueChange = {
                            calfEarTag = it
                            error = null
                        },
                        label = { Text("Calf ear tag *") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = error != null,
                        supportingText = error?.let { msg -> { Text(msg, color = androidx.compose.material3.MaterialTheme.colorScheme.error) } }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
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
                    when {
                        createCalf && calfEarTag.trim().isBlank() -> error = "Calf ear tag is required when creating calf record"
                        else -> {
                            error = null
                            val weight = calfWeightStr.toDoubleOrNull()
                            onConfirm(actualDate, assistanceRequired, calfSex, weight, createCalf, calfEarTag.trim(), notes.takeIf { it.isNotBlank() })
                            // Dialog closed by parent on success; stays open on error so user can fix
                        }
                    }
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
