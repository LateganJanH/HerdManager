package com.herdmanager.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.herdmanager.app.domain.model.HealthEventType
import java.time.LocalDate

@Composable
fun AddHealthEventDialog(
    onConfirm: (
        eventType: HealthEventType,
        date: LocalDate,
        product: String?,
        dosage: String?,
        withdrawalPeriodEnd: LocalDate?,
        notes: String?
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var eventType by remember { mutableStateOf(HealthEventType.VACCINATION) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var product by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var withdrawalDateStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showWithdrawalDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { date = it },
            onDismiss = { showDatePicker = false },
            initialDate = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
    }
    if (showWithdrawalDatePicker) {
        DatePickerDialog(
            onDateSelected = { withdrawalDateStr = it.toString() },
            onDismiss = { showWithdrawalDatePicker = false },
            initialDate = withdrawalDateStr.ifBlank { LocalDate.now().toString() }.let { s ->
                try {
                    LocalDate.parse(s).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                } catch (_: Exception) {
                    System.currentTimeMillis()
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log treatment or vaccination") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Type", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                ) {
                    HealthEventType.entries.forEach { type ->
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
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Date: $date")
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = product,
                    onValueChange = { product = it },
                    label = { Text("Product / medication") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosage") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showWithdrawalDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Withdrawal end: ${withdrawalDateStr.ifBlank { "Not set" }}")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        eventType,
                        date,
                        product.takeIf { it.isNotBlank() },
                        dosage.takeIf { it.isNotBlank() },
                        withdrawalDateStr.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
                        notes.takeIf { it.isNotBlank() }
                    )
                    onDismiss()
                }
            ) {
                Text("Save", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
