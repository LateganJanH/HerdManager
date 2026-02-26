package com.herdmanager.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import android.net.Uri
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import com.herdmanager.app.domain.model.Animal
import com.herdmanager.app.domain.model.AnimalStatus
import com.herdmanager.app.domain.model.Herd
import com.herdmanager.app.domain.model.FarmSettings
import com.herdmanager.app.domain.model.Sex
import com.herdmanager.app.ui.components.SyncStatusStrip
import com.herdmanager.app.ui.theme.WarningAmber
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class HerdCategoryFilter(val label: String) {
    ALL("All"),
    BULLS("Bulls"),
    STEERS("Steers"),
    COWS("Cows"),
    HEIFERS("Heifers"),
    CALVES("Calves")
}

private enum class AnimalSortOrder(val label: String) {
    EAR_TAG("Ear tag"),
    NAME("Name"),
    DATE_OF_BIRTH("Date of birth")
}

private const val CALF_AGE_MONTHS = 12L
private const val HEIFER_AGE_MONTHS = 24L

private fun Animal.monthsOld(): Long = ChronoUnit.MONTHS.between(dateOfBirth, LocalDate.now())

private fun Animal.matchesCategory(category: HerdCategoryFilter): Boolean = when (category) {
    HerdCategoryFilter.ALL -> true
    HerdCategoryFilter.BULLS -> sex == Sex.MALE && isCastrated != true
    HerdCategoryFilter.STEERS -> sex == Sex.MALE && isCastrated == true
    HerdCategoryFilter.COWS -> sex == Sex.FEMALE && monthsOld() >= HEIFER_AGE_MONTHS
    HerdCategoryFilter.HEIFERS -> sex == Sex.FEMALE && monthsOld() in CALF_AGE_MONTHS until HEIFER_AGE_MONTHS
    HerdCategoryFilter.CALVES -> monthsOld() < CALF_AGE_MONTHS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HerdListScreen(
    onAddAnimal: () -> Unit,
    onAnimalClick: (String) -> Unit = {},
    onNavigateToBreeding: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSummary: () -> Unit = {},
    viewModel: HerdListViewModel = hiltViewModel()
) {
    val animals by viewModel.animals.collectAsState(initial = emptyList())
    val herds by viewModel.herds.collectAsState(initial = emptyList())
    val selectedHerdId by viewModel.selectedHerdId.collectAsState(initial = null)
    val displayPhotoUriByAnimalId by viewModel.displayPhotoUriByAnimalId.collectAsState(initial = emptyMap())
    val calvingAlerts by viewModel.upcomingCalvingAlerts.collectAsState(initial = emptyList())
    val pregnancyCheckAlerts by viewModel.pregnancyCheckAlerts.collectAsState(initial = emptyList())
    val withdrawalAlerts by viewModel.withdrawalAlerts.collectAsState(initial = emptyList())
    val farmDisplayName by viewModel.farmDisplayName.collectAsState(initial = "Herd")
    val farmSettings by viewModel.farmSettings.collectAsState(initial = FarmSettings())
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<AnimalStatus?>(null) }
    var categoryFilter by remember { mutableStateOf(HerdCategoryFilter.ALL) }
    var sortOrder by remember { mutableStateOf(AnimalSortOrder.EAR_TAG) }
    val lastSyncedAt by viewModel.lastSyncedAt.collectAsState(initial = null)
    val isSyncing by viewModel.isSyncing.collectAsState(initial = false)
    val syncError by viewModel.syncError.collectAsState(initial = null)
    val isListLoading by viewModel.isListLoading.collectAsState(initial = true)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDeleteAnimal by remember { mutableStateOf<Animal?>(null) }
    val filteredAnimals = remember(animals, searchQuery, statusFilter, categoryFilter, sortOrder) {
        var list = animals
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            list = list.filter {
                it.earTagNumber.lowercase().contains(q) ||
                    (it.name?.lowercase()?.contains(q) == true) ||
                    it.breed.lowercase().contains(q)
            }
        }
        if (statusFilter != null) {
            list = list.filter { it.status == statusFilter }
        }
        list = list.filter { it.matchesCategory(categoryFilter) }
        when (sortOrder) {
            AnimalSortOrder.EAR_TAG -> list.sortedBy { it.earTagNumber.lowercase() }
            AnimalSortOrder.NAME -> list.sortedBy { it.name?.lowercase() ?: "\uFFFF" }
            AnimalSortOrder.DATE_OF_BIRTH -> list.sortedByDescending { it.dateOfBirth }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val context = LocalContext.current
            val alertCount = calvingAlerts.size + pregnancyCheckAlerts.size + withdrawalAlerts.size
            TopAppBar(
                title = {
                    Column {
                        Text(
                            farmDisplayName,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${animals.size} head" + if (alertCount > 0) " · $alertCount due soon" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                menuExpanded = false
                                onNavigateToSettings()
                            },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                        )
                        if (animals.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Export herd to CSV") },
                                onClick = {
                                    menuExpanded = false
                                    scope.launch {
                                        val csv = viewModel.exportAnimalsCsv(animals)
                                        val dateStr = LocalDate.now().toString()
                                        val filename = "HerdManager-herd-$dateStr.csv"
                                        val file = File(context.cacheDir, filename)
                                        withContext(Dispatchers.IO) { file.writeText(csv) }
                                        withContext(Dispatchers.Main) {
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )
                                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "text/csv"
                                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(
                                                android.content.Intent.createChooser(intent, "Export herd to CSV")
                                            )
                                        }
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = "Export herd to CSV") }
                            )
                            DropdownMenuItem(
                                text = { Text("Export herd to PDF") },
                                onClick = {
                                    menuExpanded = false
                                    scope.launch {
                                        val bytes = withContext(Dispatchers.IO) { viewModel.exportAnimalsPdf(animals) }
                                        val dateStr = LocalDate.now().toString()
                                        val filename = "HerdManager-herd-$dateStr.pdf"
                                        val file = File(context.cacheDir, filename)
                                        withContext(Dispatchers.IO) { file.writeBytes(bytes) }
                                        withContext(Dispatchers.Main) {
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )
                                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(
                                                android.content.Intent.createChooser(intent, "Export herd to PDF")
                                            )
                                        }
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = "Export herd to PDF") }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddAnimal,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Register animal")
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Text("Register animal")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("herd_list_screen")
        ) {
            SyncStatusStrip(
                lastSyncedAt = lastSyncedAt,
                isSyncing = isSyncing,
                syncError = syncError,
                formatLastSynced = { viewModel.formatLastSynced(it) },
                onSync = { viewModel.syncNow() },
                onDismissError = { viewModel.clearSyncError() }
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Ear tag, name or breed") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HerdFilterDropdown(
                    herds = herds,
                    selectedHerdId = selectedHerdId,
                    onHerdSelected = { viewModel.setSelectedHerd(it) }
                )
                StatusFilterDropdown(
                    selectedStatus = statusFilter,
                    onStatusSelected = { statusFilter = it }
                )
                CategoryFilterDropdown(
                    selectedCategory = categoryFilter,
                    onCategorySelected = { categoryFilter = it }
                )
                SortOrderDropdown(
                    selectedSortOrder = sortOrder,
                    onSortOrderSelected = { sortOrder = it }
                )
                val hasActiveFilters = searchQuery.isNotBlank() ||
                    statusFilter != null ||
                    categoryFilter != HerdCategoryFilter.ALL ||
                    selectedHerdId != null
                if (hasActiveFilters) {
                    TextButton(
                        onClick = {
                            searchQuery = ""
                            statusFilter = null
                            categoryFilter = HerdCategoryFilter.ALL
                            viewModel.setSelectedHerd(null)
                        }
                    ) {
                        Text("Clear filters", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            HerdSummaryStrip(
                herdCount = animals.size,
                dueSoonCount = calvingAlerts.size + pregnancyCheckAlerts.size + withdrawalAlerts.size,
                onDueSoonClick = onNavigateToBreeding
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (pregnancyCheckAlerts.isNotEmpty()) {
                val msg = if (pregnancyCheckAlerts.size == 1)
                    "${pregnancyCheckAlerts.first().damEarTag} pregnancy check due"
                else
                    "${pregnancyCheckAlerts.size} pregnancy checks due"
                AlertBanner(
                    title = "Pregnancy check due",
                    message = msg,
                    onClick = onNavigateToBreeding,
                    icon = Icons.Default.MedicalServices
                )
            }
            if (calvingAlerts.isNotEmpty()) {
                val alertDays = farmSettings.calvingAlertDaysClamped()
                CalvingAlertBanner(
                    alertCount = calvingAlerts.size,
                    message = if (calvingAlerts.size == 1)
                        "${calvingAlerts.first().damEarTag} due in ${calvingAlerts.first().daysUntilDue} days"
                    else
                        "${calvingAlerts.size} calvings due within $alertDays days",
                    onClick = onNavigateToBreeding
                )
            }
            if (withdrawalAlerts.isNotEmpty()) {
                val msg = if (withdrawalAlerts.size == 1)
                    "${withdrawalAlerts.first().earTag} withdrawal ends in ${withdrawalAlerts.first().daysUntilDue} days"
                else
                    "${withdrawalAlerts.size} withdrawals end within 14 days"
                AlertBanner(
                    title = "Withdrawal ends",
                    message = msg,
                    onClick = onNavigateToBreeding,
                    icon = Icons.Default.MedicalServices
                )
            }
            syncError?.let { err ->
                Text(
                    err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Text(
                    "Dismiss",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .clickable { viewModel.clearSyncError() }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            if (isListLoading) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(6) { HerdListSkeletonItem() }
                }
            } else if (filteredAnimals.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    EmptyHerdState(
                        onAddAnimal = onAddAnimal,
                        onClearSearch = { searchQuery = "" },
                        hasAnimals = animals.isNotEmpty(),
                        searchQuery = searchQuery
                    )
                }
            } else {
                PullToRefreshBox(
                    modifier = Modifier.weight(1f),
                    isRefreshing = isSyncing,
                    onRefresh = { viewModel.syncNow() }
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredAnimals, key = { it.id }) { animal ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        pendingDeleteAnimal = animal
                                        false
                                    } else false
                                }
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                modifier = Modifier.fillMaxWidth().testTag("herd_item_${animal.earTagNumber}"),
                                enableDismissFromStartToEnd = false,
                                backgroundContent = {
                                    if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.error)
                                                .padding(horizontal = 20.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Remove",
                                                tint = MaterialTheme.colorScheme.onError
                                            )
                                        }
                                    }
                                }
                            ) {
                                AnimalCard(
                                    animal = animal,
                                    displayPhotoUri = displayPhotoUriByAnimalId[animal.id],
                                    onClick = { onAnimalClick(animal.id) }
                                )
                            }
                        }
                    }
                }
            }
            pendingDeleteAnimal?.let { animal ->
                AlertDialog(
                    onDismissRequest = { pendingDeleteAnimal = null },
                    title = { Text("Remove animal?") },
                    text = { Text("${animal.earTagNumber} will be removed from the herd list.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteAnimal(animal.id)
                                pendingDeleteAnimal = null
                                scope.launch {
                                    snackbarHostState.showSnackbar("Animal removed")
                                }
                            }
                        ) {
                            Text("Remove", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingDeleteAnimal = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HerdFilterDropdown(
    herds: List<Herd>,
    selectedHerdId: String?,
    onHerdSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (selectedHerdId) {
        null -> "All herds"
        "unassigned" -> "Unassigned"
        else -> herds.find { it.id == selectedHerdId }?.name ?: "All herds"
    }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true).widthIn(min = 120.dp),
            shape = RoundedCornerShape(12.dp)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("All herds") }, onClick = { onHerdSelected(null); expanded = false })
            DropdownMenuItem(text = { Text("Unassigned") }, onClick = { onHerdSelected("unassigned"); expanded = false })
            herds.forEach { herd ->
                DropdownMenuItem(text = { Text(herd.name) }, onClick = { onHerdSelected(herd.id); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusFilterDropdown(
    selectedStatus: AnimalStatus?,
    onStatusSelected: (AnimalStatus?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val statusLabel = selectedStatus?.name?.replace('_', ' ') ?: "All statuses"
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = statusLabel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                .widthIn(min = 140.dp),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All statuses") },
                onClick = {
                    onStatusSelected(null)
                    expanded = false
                }
            )
            AnimalStatus.entries.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.name.replace('_', ' ')) },
                    onClick = {
                        onStatusSelected(status)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterDropdown(
    selectedCategory: HerdCategoryFilter,
    onCategorySelected: (HerdCategoryFilter) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedCategory.label,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                .widthIn(min = 120.dp),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            HerdCategoryFilter.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.label) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortOrderDropdown(
    selectedSortOrder: AnimalSortOrder,
    onSortOrderSelected: (AnimalSortOrder) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedSortOrder.label,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                .widthIn(min = 120.dp),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AnimalSortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(order.label) },
                    onClick = {
                        onSortOrderSelected(order)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun HerdSummaryStrip(
    herdCount: Int,
    dueSoonCount: Int,
    onDueSoonClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "$herdCount head",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        if (dueSoonCount > 0) {
            androidx.compose.material3.Surface(
                color = WarningAmber.copy(alpha = 0.25f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "$dueSoonCount due soon",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clickable(onClick = onDueSoonClick)
                )
            }
        }
    }
}

@Composable
private fun AlertBanner(
    title: String,
    message: String,
    onClick: () -> Unit,
    icon: ImageVector
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun CalvingAlertBanner(
    alertCount: Int,
    message: String,
    onClick: () -> Unit
) {
    AlertBanner(
        title = "Calving due",
        message = message,
        onClick = onClick,
        icon = Icons.Default.Warning
    )
}

@Composable
private fun EmptyHerdState(
    onAddAnimal: () -> Unit,
    onClearSearch: () -> Unit = {},
    hasAnimals: Boolean = false,
    searchQuery: String = ""
) {
    val isSearchNoMatch = hasAnimals && searchQuery.isNotBlank()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!hasAnimals || isSearchNoMatch) {
            Icon(
                imageVector = Icons.Default.Pets,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
        Text(
            text = if (isSearchNoMatch)
                "No cattle match \"$searchQuery\""
            else if (hasAnimals)
                "No cattle in this list"
            else
                "No cattle recorded yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isSearchNoMatch)
                "Try a different search or clear filters"
            else
                "Register your first animal to start tracking your herd",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!hasAnimals) {
            Spacer(modifier = Modifier.height(24.dp))
            androidx.compose.material3.Button(
                onClick = onAddAnimal,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Register animal")
            }
        } else if (isSearchNoMatch) {
            Spacer(modifier = Modifier.height(24.dp))
            androidx.compose.material3.Button(
                onClick = onClearSearch,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Clear search")
            }
        }
    }
}

@Composable
private fun AnimalCard(
    animal: Animal,
    displayPhotoUri: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, role = Role.Button, onClickLabel = "Open ${animal.earTagNumber}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                if (displayPhotoUri != null) {
                    val imageUri = if (displayPhotoUri.startsWith("http")) Uri.parse(displayPhotoUri) else Uri.parse("file://$displayPhotoUri")
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Animal photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    androidx.compose.material3.Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Pets,
                                contentDescription = "No photo",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = animal.earTagNumber,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                animal.name?.let { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${animal.sex} · ${animal.breed}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            androidx.compose.material3.Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = animal.status.name.replace('_', ' '),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun HerdListSkeletonItem() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .height(18.dp)
                        .width(120.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .height(14.dp)
                        .width(180.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                )
            }
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .width(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            )
        }
    }
}
