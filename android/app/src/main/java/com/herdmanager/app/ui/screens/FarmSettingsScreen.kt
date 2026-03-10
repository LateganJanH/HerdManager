package com.herdmanager.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
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
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.herdmanager.app.BuildConfig
import com.herdmanager.app.domain.model.FarmSettings
import kotlinx.coroutines.tasks.await
import com.herdmanager.app.domain.model.FarmContact
import com.herdmanager.app.domain.model.ThemeMode
import com.herdmanager.app.domain.model.Herd
import com.herdmanager.app.ui.components.HorizontalFilterChips
import com.herdmanager.app.ui.components.SyncStatusStrip
import java.io.File
import java.time.LocalDate

private enum class SettingsTab(val label: String) {
    FARM("Farm"),
    OPERATIONS("Operations"),
    HERDS("Herds"),
    SYNC("Sync"),
    SYSTEM("System"),
    DATA("Data"),
    ABOUT("About")
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

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
    var weaningAgeDays by remember { mutableStateOf("200") }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var herdToDelete by remember { mutableStateOf<Herd?>(null) }
    var newHerdName by remember { mutableStateOf("") }
    var currencyCode by remember { mutableStateOf(FarmSettings.DEFAULT_CURRENCY_CODE) }
    var selectedTab by remember { mutableStateOf(SettingsTab.FARM) }
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
        currencyCode = settings.currencyCode.ifBlank { FarmSettings.DEFAULT_CURRENCY_CODE }
        gestationDays = settings.gestationDays.toString()
        calvingAlertDays = settings.calvingAlertDays.toString()
        pregnancyCheckDays = settings.pregnancyCheckDaysAfterBreeding.toString()
        weaningAgeDays = settings.weaningAgeDays.toString()
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
                .fillMaxSize()
                .padding(padding)
                .testTag("farm_settings_screen")
        ) {
            // Tab row
            HorizontalFilterChips(
                options = SettingsTab.entries.map { tab -> tab.label to (selectedTab == tab) },
                onOptionSelected = { index ->
                    if (index in SettingsTab.entries.indices) {
                        selectedTab = SettingsTab.entries[index]
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.Start
            ) {
                when (selectedTab) {
                    SettingsTab.FARM -> {
                        Text(
                            text = "Farm settings",
                            style = MaterialTheme.typography.titleMedium,
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
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Contacts",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Currency (for transactions)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalFilterChips(
                            options = com.herdmanager.app.ui.util.CurrencyFormat.supportedCurrencies().map { (code, label) ->
                                label to (currencyCode == code)
                            },
                            onOptionSelected = { index ->
                                currencyCode = com.herdmanager.app.ui.util.CurrencyFormat.supportedCurrencies()[index].first
                            }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val days = calvingAlertDays.toIntOrNull() ?: FarmSettings.DEFAULT_CALVING_ALERT_DAYS
                                viewModel.updateFarmSettings(
                                    FarmSettings(
                                        name = name.trim(),
                                        address = address.trim(),
                                        contacts = contacts.map { it.copy(name = it.name.trim(), phone = it.phone.trim(), email = it.email.trim()) },
                                        calvingAlertDays = days.coerceIn(FarmSettings.CALVING_ALERT_DAYS_MIN, FarmSettings.CALVING_ALERT_DAYS_MAX),
                                        pregnancyCheckDaysAfterBreeding = (pregnancyCheckDays.toIntOrNull() ?: FarmSettings.DEFAULT_PREGNANCY_CHECK_DAYS).coerceIn(FarmSettings.PREGNANCY_CHECK_DAYS_MIN, FarmSettings.PREGNANCY_CHECK_DAYS_MAX),
                                        gestationDays = (gestationDays.toIntOrNull() ?: FarmSettings.DEFAULT_GESTATION_DAYS).coerceIn(FarmSettings.GESTATION_DAYS_MIN, FarmSettings.GESTATION_DAYS_MAX),
                                        weaningAgeDays = (weaningAgeDays.toIntOrNull() ?: FarmSettings.DEFAULT_WEANING_AGE_DAYS).coerceIn(FarmSettings.WEANING_AGE_DAYS_MIN, FarmSettings.WEANING_AGE_DAYS_MAX),
                                        currencyCode = currencyCode.ifBlank { FarmSettings.DEFAULT_CURRENCY_CODE }
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
                    }
                    SettingsTab.OPERATIONS -> {
                        Text(
                            text = "Farm operations",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Calving, pregnancy check, gestation and weaning reminders. Save from Farm tab.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = calvingAlertDays,
                            onValueChange = { newVal -> if (newVal.isEmpty() || newVal.all { it.isDigit() }) calvingAlertDays = newVal },
                            label = { Text("Calving reminder window (days)") },
                            placeholder = { Text("14") },
                            supportingText = { Text("Remind ${FarmSettings.CALVING_ALERT_DAYS_MIN}–${FarmSettings.CALVING_ALERT_DAYS_MAX} days before due", style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = pregnancyCheckDays,
                            onValueChange = { newVal -> if (newVal.isEmpty() || newVal.all { it.isDigit() }) pregnancyCheckDays = newVal },
                            label = { Text("Pregnancy check (days after service)") },
                            placeholder = { Text("45") },
                            supportingText = { Text("${FarmSettings.PREGNANCY_CHECK_DAYS_MIN}–${FarmSettings.PREGNANCY_CHECK_DAYS_MAX} days", style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = gestationDays,
                            onValueChange = { newVal -> if (newVal.isEmpty() || newVal.all { it.isDigit() }) gestationDays = newVal },
                            label = { Text("Gestation length (days)") },
                            placeholder = { Text("283") },
                            supportingText = { Text("${FarmSettings.GESTATION_DAYS_MIN}–${FarmSettings.GESTATION_DAYS_MAX}, cattle default 283", style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = weaningAgeDays,
                            onValueChange = { newVal -> if (newVal.isEmpty() || newVal.all { it.isDigit() }) weaningAgeDays = newVal },
                            label = { Text("Weaning age (days)") },
                            placeholder = { Text("200") },
                            supportingText = { Text("Alert when weaning weight is due", style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    SettingsTab.HERDS -> {
                        Text(
                            text = "Herds",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Manage herds for bull introduction, quarantine, grazing rotation, etc.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
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
                            ) { Text("Add") }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        herds.forEach { herd ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
                    }
                    SettingsTab.SYNC -> {
                        Text(
                            text = "Sync settings",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SyncStatusStrip(
                            lastSyncedAt = lastSyncedAt,
                            isSyncing = isSyncing,
                            syncError = syncError,
                            formatLastSynced = { viewModel.formatLastSynced(it) },
                            onSync = { viewModel.syncNow() },
                            onDismissError = { viewModel.clearSyncError() }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { viewModel.syncNow() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isSyncing
                        ) {
                            Text(if (isSyncing) "Syncing…" else "Sync now")
                        }
                    }
                    SettingsTab.SYSTEM -> {
                        Text(
                            text = "System settings",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
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
                    }
                    SettingsTab.DATA -> {
                        Text(
                            text = "Data",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Export or restore your herd data.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val json = viewModel.exportBackupJson()
                                    withContext(Dispatchers.Main) {
                                        val dateStr = LocalDate.now().toString()
                                        val file = File(context.cacheDir, "HerdManager-backup-$dateStr.json")
                                        file.writeText(json)
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
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
                    }
                    SettingsTab.ABOUT -> {
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
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
                        if (BuildConfig.SOLUTION_ID.isNotBlank()) {
                            Text(
                                text = "Instance: ${BuildConfig.SOLUTION_ID}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("settings-about-solution-id")
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                val activity = context.findActivity() ?: return@OutlinedButton
                                checkingUpdate = true
                                scope.launch {
                                    try {
                                        val appUpdateManager = AppUpdateManagerFactory.create(context)
                                        val appUpdateInfo = appUpdateManager.appUpdateInfo.await()
                                        if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                                            val updateType = when {
                                                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
                                                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
                                                else -> null
                                            }
                                            if (updateType != null) {
                                                appUpdateManager.startUpdateFlow(appUpdateInfo, activity, AppUpdateOptions.newBuilder(updateType).build())
                                            } else {
                                                snackbarHostState.showSnackbar("No update available")
                                            }
                                        } else {
                                            snackbarHostState.showSnackbar("App is up to date")
                                        }
                                    } catch (_: Exception) {
                                        snackbarHostState.showSnackbar("Could not check for updates")
                                    }
                                    checkingUpdate = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !checkingUpdate
                        ) {
                            Text(if (checkingUpdate) "Checking…" else "Check for updates")
                        }
                        if (BuildConfig.SUPPORT_BASE_URL.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val supportBase = BuildConfig.SUPPORT_BASE_URL.trimEnd('/')
                            val solutionId = BuildConfig.SOLUTION_ID
                            fun openSupport(topic: String? = null) {
                                val query = buildString {
                                    append("solutionId=${Uri.encode(solutionId)}")
                                    if (!topic.isNullOrBlank()) append("&topic=$topic")
                                }
                                val sep = if (supportBase.contains("?")) "&" else "?"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("$supportBase$sep$query")))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(onClick = { openSupport(null) }) { Text("Help & support", style = MaterialTheme.typography.bodySmall) }
                                TextButton(onClick = { openSupport("suggest") }) { Text("Suggest a feature", style = MaterialTheme.typography.bodySmall) }
                                TextButton(onClick = { openSupport("report") }) { Text("Report a problem", style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                }
            }
        }
    }
}
