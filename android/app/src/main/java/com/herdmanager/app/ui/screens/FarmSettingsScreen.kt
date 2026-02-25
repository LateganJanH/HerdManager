package com.herdmanager.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.FileProvider
import com.herdmanager.app.BuildConfig
import com.herdmanager.app.domain.model.FarmSettings
import com.herdmanager.app.domain.model.FarmContact
import com.herdmanager.app.domain.model.ThemeMode
import com.herdmanager.app.domain.model.Herd
import com.herdmanager.app.ui.components.SyncStatusStrip
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: FarmSettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val settings by viewModel.farmSettings.collectAsState()
    val herds by viewModel.herds.collectAsState()
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf<List<FarmContact>>(emptyList()) }
    var calvingAlertDays by remember { mutableStateOf("14") }
    var pregnancyCheckDays by remember { mutableStateOf("45") }
    var gestationDays by remember { mutableStateOf("283") }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var herdToDelete by remember { mutableStateOf<Herd?>(null) }
    var newHerdName by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeMode by themeViewModel.themeMode.collectAsState(ThemeMode.SYSTEM)
    val lastSyncedAt by viewModel.lastSyncedAt.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState(initial = false)
    val syncError by viewModel.syncError.collectAsState(initial = null)

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() } ?: ""
                    }
                    viewModel.importFromJson(json)
                        .onSuccess {
                            snackbarHostState.showSnackbar("Data restored")
                            delay(1500)
                            onNavigateBack()
                        }
                        .onFailure {
                            snackbarHostState.showSnackbar("Restore failed: ${it.message}")
                        }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Restore failed: ${e.message}")
                }
            }
        }
    }

    herdToDelete?.let { herd ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { herdToDelete = null },
            title = { Text("Remove herd?") },
            text = { Text("Remove \"${herd.name}\"? Animals in this herd will be unassigned (no longer in a herd).") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteHerd(herd)
                    herdToDelete = null
                    scope.launch { snackbarHostState.showSnackbar("Herd removed") }
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { herdToDelete = null }) { Text("Cancel") }
            }
        )
    }
    if (showRestoreConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore from backup?") },
            text = { Text("This will replace all current data with the backup. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    restoreLauncher.launch(arrayOf("application/json", "*/*"))
                }) {
                    Text("Restore", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    if (showSignOutConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Sign out?") },
            text = { Text("You will need to sign in again to access your herd data.") },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutConfirm = false
                    authViewModel.signOut()
                }) {
                    Text("Sign out", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) { Text("Cancel") }
            }
        )
    }

    LaunchedEffect(settings) {
        name = settings.name
        address = settings.address
        contacts = settings.contacts
        gestationDays = settings.gestationDays.toString()
        calvingAlertDays = settings.calvingAlertDays.toString()
        pregnancyCheckDays = settings.pregnancyCheckDaysAfterBreeding.toString()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Farm profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to herd")
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
                .padding(horizontal = 16.dp)
        ) {
            SyncStatusStrip(
                lastSyncedAt = lastSyncedAt,
                isSyncing = isSyncing,
                syncError = syncError,
                formatLastSynced = { viewModel.formatLastSynced(it) },
                onSync = { viewModel.syncNow() },
                onDismissError = { viewModel.clearSyncError() }
            )
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { themeViewModel.setThemeMode(mode) },
                        label = { Text(mode.label) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Farm information",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Farm Name") },
                placeholder = { Text("My Farm") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Contacts",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            contacts.forEachIndexed { index, c ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = c.name,
                        onValueChange = { contacts = contacts.toMutableList().apply { set(index, c.copy(name = it)) } },
                        label = { Text("Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = c.phone,
                        onValueChange = { contacts = contacts.toMutableList().apply { set(index, c.copy(phone = it)) } },
                        label = { Text("Phone") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = c.email,
                        onValueChange = { contacts = contacts.toMutableList().apply { set(index, c.copy(email = it)) } },
                        label = { Text("Email") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    IconButton(onClick = { contacts = contacts.filterIndexed { i, _ -> i != index } }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove contact")
                    }
                }
            }
            OutlinedButton(
                onClick = { contacts = contacts + FarmContact() },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add contact")
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Calving & reminders",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = calvingAlertDays,
                onValueChange = { newVal ->
                    if (newVal.isEmpty() || newVal.all { it.isDigit() }) {
                        calvingAlertDays = newVal
                    }
                },
                label = { Text("Calving reminder window (days)") },
                placeholder = { Text("14") },
                supportingText = {
                    Text(
                        text = "Remind ${FarmSettings.CALVING_ALERT_DAYS_MIN}–${FarmSettings.CALVING_ALERT_DAYS_MAX} days before due date",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = pregnancyCheckDays,
                onValueChange = { newVal ->
                    if (newVal.isEmpty() || newVal.all { it.isDigit() }) {
                        pregnancyCheckDays = newVal
                    }
                },
                label = { Text("Pregnancy check (days after service)") },
                placeholder = { Text("45") },
                supportingText = {
                    Text(
                        text = "Remind when pregnancy check is due (${FarmSettings.PREGNANCY_CHECK_DAYS_MIN}–${FarmSettings.PREGNANCY_CHECK_DAYS_MAX} days)",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = gestationDays,
                onValueChange = { newVal ->
                    if (newVal.isEmpty() || newVal.all { it.isDigit() }) {
                        gestationDays = newVal
                    }
                },
                label = { Text("Gestation length (days)") },
                placeholder = { Text("283") },
                supportingText = {
                    Text(
                        text = "Used for due date from service date (${FarmSettings.GESTATION_DAYS_MIN}–${FarmSettings.GESTATION_DAYS_MAX}, cattle default 283)",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Herds",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Manage herds for bull introduction, quarantine, grazing rotation, etc.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newHerdName,
                    onValueChange = { newHerdName = it },
                    label = { Text("New herd name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Button(
                    onClick = {
                        if (newHerdName.isNotBlank()) {
                            viewModel.addHerd(newHerdName)
                            newHerdName = ""
                            scope.launch { snackbarHostState.showSnackbar("Herd added") }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            herds.forEach { herd ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(herd.name, style = MaterialTheme.typography.bodyMedium)
                        herd.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    IconButton(onClick = { herdToDelete = herd }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete herd")
                    }
                }
            }
            if (herds.isEmpty()) {
                Text(
                    "No herds yet. Add herds to organize animals (e.g. main herd, quarantine, young bulls).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Data",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val json = viewModel.exportBackupJson()
                        withContext(Dispatchers.Main) {
                            val dateStr = LocalDate.now().toString()
                            val filename = "HerdManager-backup-$dateStr.json"
                            val file = File(context.cacheDir, filename)
                            file.writeText(json)
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Backup data"))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Backup data (export JSON)")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showRestoreConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Restore from backup")
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    val days = calvingAlertDays.toIntOrNull()
                        ?: FarmSettings.DEFAULT_CALVING_ALERT_DAYS
                    viewModel.updateFarmSettings(
                        FarmSettings(
                            name = name.trim(),
                            address = address.trim(),
                            contacts = contacts.map { it.copy(name = it.name.trim(), phone = it.phone.trim(), email = it.email.trim()) },
                            calvingAlertDays = days.coerceIn(
                                FarmSettings.CALVING_ALERT_DAYS_MIN,
                                FarmSettings.CALVING_ALERT_DAYS_MAX
                            ),
                            pregnancyCheckDaysAfterBreeding = (pregnancyCheckDays.toIntOrNull()
                                ?: FarmSettings.DEFAULT_PREGNANCY_CHECK_DAYS).coerceIn(
                                FarmSettings.PREGNANCY_CHECK_DAYS_MIN,
                                FarmSettings.PREGNANCY_CHECK_DAYS_MAX
                            ),
                            gestationDays = (gestationDays.toIntOrNull()
                                ?: FarmSettings.DEFAULT_GESTATION_DAYS).coerceIn(
                                FarmSettings.GESTATION_DAYS_MIN,
                                FarmSettings.GESTATION_DAYS_MAX
                            )
                        )
                    )
                    scope.launch {
                        snackbarHostState.showSnackbar("Settings saved")
                        delay(1200)
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Sync",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.syncNow() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSyncing
            ) {
                Text(if (isSyncing) "Syncing…" else "Sync now")
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showSignOutConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Sign out")
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "About",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "HerdManager — cattle herd management for the field.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "App version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
