package com.herdmanager.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.herdmanager.app.domain.model.PregnancyCheckResult
import java.time.LocalDate

@Composable
fun RecordPregnancyCheckDialog(
    serviceDate: LocalDate,
    dueDate: LocalDate,
    onConfirm: (LocalDate, PregnancyCheckResult) -> Unit,
    onDismiss: () -> Unit
) {
    var checkDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedResult by remember { mutableStateOf<PregnancyCheckResult?>(null) }

    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { checkDate = it },
            onDismiss = { showDatePicker = false },
            initialDate = checkDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record pregnancy check") },
        text = {
            Column {
                Text(
                    "Service: $serviceDate · Due: $dueDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { showDatePicker = true }) {
                        Text("Check date: $checkDate")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Result", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    PregnancyCheckResult.entries.forEach { result ->
                        TextButton(
                            onClick = { selectedResult = result },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = result.name.replace('_', ' '),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedResult == result) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedResult?.let { onConfirm(checkDate, it) }
                },
                enabled = selectedResult != null
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
