package com.herdmanager.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.herdmanager.app.domain.model.WeightRecord
import java.time.LocalDate

@Composable
fun LogWeightDialog(
    initialDate: LocalDate,
    onConfirm: (recordId: String?, LocalDate, Double, String?) -> Unit,
    onDismiss: () -> Unit,
    existing: WeightRecord? = null
) {
    var date by remember(existing) { mutableStateOf(existing?.date ?: initialDate) }
    var weightStr by remember(existing) { mutableStateOf(existing?.weightKg?.toString() ?: "") }
    var note by remember(existing) { mutableStateOf(existing?.note ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { date = it },
            onDismiss = { showDatePicker = false },
            initialDate = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Edit weight" else "Log weight") },
        text = {
            Column(modifier = Modifier.widthIn(min = 280.dp)) {
                Button(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Date: $date")
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = weightStr,
                    onValueChange = {
                        weightStr = it
                        error = null
                    },
                    label = { Text("Weight (kg) *") },
                    singleLine = true,
                    modifier = Modifier.widthIn(min = 200.dp),
                    shape = RoundedCornerShape(12.dp),
                    isError = error != null,
                    supportingText = error?.let { msg -> { Text(msg, color = androidx.compose.material3.MaterialTheme.colorScheme.error) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (e.g. weaning)") },
                    singleLine = true,
                    modifier = Modifier.widthIn(min = 200.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = weightStr.trim()
                    when {
                        trimmed.isEmpty() -> error = "Weight is required"
                        trimmed.toDoubleOrNull() == null -> error = "Enter a valid number"
                        trimmed.toDoubleOrNull()!! <= 0 -> error = "Weight must be greater than 0"
                        else -> {
                            val weightKg = trimmed.toDouble()
                            onConfirm(existing?.id, date, weightKg, note.takeIf { it.isNotBlank() })
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (existing != null) "Update" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
