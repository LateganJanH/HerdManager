package com.herdmanager.app.ui.components

import androidx.compose.foundation.layout.Column
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
import com.herdmanager.app.domain.model.Herd
import java.time.LocalDate

@Composable
fun TransferHerdDialog(
    currentHerdId: String?,
    currentHerdName: String?,
    herds: List<Herd>,
    onConfirm: (String, LocalDate, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHerdId by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var transferDate by remember { mutableStateOf(LocalDate.now()) }
    var reason by remember { mutableStateOf("") }

    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { transferDate = it },
            onDismiss = { showDatePicker = false },
            initialDate = transferDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer to herd") },
        text = {
            val targetHerds = herds.filter { it.id != currentHerdId }
            Column {
                currentHerdName?.let { Text("Current: $it", style = MaterialTheme.typography.bodyMedium) }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Transfer to", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                if (targetHerds.isEmpty()) {
                    Text(
                        "No other herds. Add herds in Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    targetHerds.forEach { herd ->
                        androidx.compose.material3.TextButton(
                            onClick = { selectedHerdId = herd.id },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                herd.name,
                                color = if (selectedHerdId == herd.id) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { showDatePicker = true }) {
                    Text("Transfer date: $transferDate")
                }
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val targetHerds = herds.filter { it.id != currentHerdId }
            Button(
                onClick = {
                    selectedHerdId?.let { herdId ->
                        onConfirm(herdId, transferDate, reason.takeIf { it.isNotBlank() })
                    }
                },
                enabled = selectedHerdId != null && targetHerds.isNotEmpty()
            ) {
                Text("Transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
