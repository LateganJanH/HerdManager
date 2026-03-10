package com.herdmanager.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PregnantWoman
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.herdmanager.app.ui.theme.UiDefaults
import com.herdmanager.app.ui.components.HorizontalFilterChips
import com.herdmanager.app.domain.model.BreedingEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AlertsFilter(val label: String) {
    ALL("All"),
    CALVING("Calving"),
    PREGNANCY_CHECK("Pregnancy check"),
    WITHDRAWAL("Withdrawal"),
    WEANING_WEIGHT("Weaning weight")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreedingScreen(
    onAnimalClick: (String) -> Unit = {},
    onNavigateToProfiles: () -> Unit = {},
    viewModel: BreedingViewModel = hiltViewModel()
) {
    val calvings by viewModel.upcomingCalvings.collectAsState(initial = emptyList())
    val pregnancyChecks by viewModel.pregnancyCheckDue.collectAsState(initial = emptyList())
    val withdrawals by viewModel.withdrawalDue.collectAsState(initial = emptyList())
    val weaningWeightDue by viewModel.weaningWeightDue.collectAsState(initial = emptyList())
    var filter by remember { mutableStateOf(AlertsFilter.ALL) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val showCalving = filter == AlertsFilter.ALL || filter == AlertsFilter.CALVING
    val showPregnancyCheck = filter == AlertsFilter.ALL || filter == AlertsFilter.PREGNANCY_CHECK
    val showWithdrawal = filter == AlertsFilter.ALL || filter == AlertsFilter.WITHDRAWAL
    val showWeaningWeight = filter == AlertsFilter.ALL || filter == AlertsFilter.WEANING_WEIGHT

    // Whether there is at least one alert for the current filter selection.
    val hasAny = when (filter) {
        AlertsFilter.ALL -> calvings.isNotEmpty() || pregnancyChecks.isNotEmpty() || withdrawals.isNotEmpty() || weaningWeightDue.isNotEmpty()
        AlertsFilter.CALVING -> calvings.isNotEmpty()
        AlertsFilter.PREGNANCY_CHECK -> pregnancyChecks.isNotEmpty()
        AlertsFilter.WITHDRAWAL -> withdrawals.isNotEmpty()
        AlertsFilter.WEANING_WEIGHT -> weaningWeightDue.isNotEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Alerts") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Calving due, pregnancy check, withdrawal and weaning weight reminders",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalFilterChips(
                    options = AlertsFilter.entries.map { it.label to (filter == it) },
                    onOptionSelected = { filter = AlertsFilter.entries[it] },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!hasAny) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PregnantWoman,
                        contentDescription = "No alerts",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No alerts right now",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Open an animal to record service or health events.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    androidx.compose.material3.Button(
                        onClick = onNavigateToProfiles,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("View herd (Profiles)")
                    }
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        scope.launch {
                            isRefreshing = true
                            viewModel.refresh()
                            delay(400)
                            isRefreshing = false
                        }
                    }
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (showCalving && calvings.isNotEmpty()) {
                            if (filter == AlertsFilter.ALL) {
                                item(key = "header_calving") {
                                    Row(
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PregnantWoman,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Calving due",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            items(calvings, key = { "calving_${it.event.id}" }) { item ->
                                GestationCard(
                                    event = item.event,
                                    damEarTag = item.damEarTag,
                                    dueDate = item.dueDate,
                                    daysUntilDue = item.daysUntilDue,
                                    onClick = { onAnimalClick(item.event.animalId) }
                                )
                            }
                        }
                        if (showPregnancyCheck && pregnancyChecks.isNotEmpty()) {
                            if (filter == AlertsFilter.ALL) {
                                item(key = "header_preg") {
                                    Row(
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PregnantWoman,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Pregnancy check due",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            items(pregnancyChecks, key = { "preg_${it.event.id}" }) { item ->
                                PregnancyCheckDueCard(
                                    damEarTag = item.damEarTag,
                                    checkDueDate = item.checkDueDate,
                                    daysUntilDue = item.daysUntilDue,
                                    onClick = { onAnimalClick(item.event.animalId) }
                                )
                            }
                        }
                        if (showWithdrawal && withdrawals.isNotEmpty()) {
                            if (filter == AlertsFilter.ALL) {
                                item(key = "header_withdrawal") {
                                    Row(
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PregnantWoman,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Withdrawal ends",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            items(withdrawals, key = { "wd_${it.event.id}" }) { item ->
                                WithdrawalDueCard(
                                    earTag = item.earTag,
                                    endDate = item.endDate,
                                    daysUntilDue = item.daysUntilDue,
                                    product = item.event.product,
                                    onClick = { onAnimalClick(item.event.animalId) }
                                )
                            }
                        }
                        if (showWeaningWeight && weaningWeightDue.isNotEmpty()) {
                            if (filter == AlertsFilter.ALL) {
                                item(key = "header_weaning") {
                                    Row(
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PregnantWoman,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Weaning weight due",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            items(weaningWeightDue, key = { "weaning_${it.animalId}" }) { item ->
                                WeaningWeightDueCard(
                                    earTag = item.earTag,
                                    weaningDueDate = item.weaningDueDate,
                                    daysUntilDue = item.daysUntilDue,
                                    onClick = { onAnimalClick(item.animalId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PregnancyCheckDueCard(
    damEarTag: String,
    checkDueDate: java.time.LocalDate,
    daysUntilDue: Long,
    onClick: () -> Unit = {}
) {
    val statusColor = when {
        daysUntilDue < 0 -> MaterialTheme.colorScheme.error
        daysUntilDue <= 7 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val dueLabel = when {
        daysUntilDue < 0 -> "Overdue by ${-daysUntilDue} days"
        daysUntilDue == 0L -> "Due today"
        else -> "$daysUntilDue days until check"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, role = Role.Button, onClickLabel = "Open $damEarTag, $dueLabel"),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = UiDefaults.CardShape
    ) {
        Column(modifier = Modifier.padding(UiDefaults.CardInnerPadding)) {
            Text(
                text = damEarTag,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Pregnancy check due: $checkDueDate",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = dueLabel,
                style = MaterialTheme.typography.labelLarge,
                color = statusColor
            )
        }
    }
}

@Composable
private fun WithdrawalDueCard(
    earTag: String,
    endDate: java.time.LocalDate,
    daysUntilDue: Long,
    product: String?,
    onClick: () -> Unit = {}
) {
    val statusColor = when {
        daysUntilDue < 0 -> MaterialTheme.colorScheme.error
        daysUntilDue <= 7 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val dueLabel = when {
        daysUntilDue < 0 -> "Overdue by ${-daysUntilDue} days"
        daysUntilDue == 0L -> "Ends today"
        else -> "$daysUntilDue days until withdrawal ends"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, role = Role.Button, onClickLabel = "Open $earTag, $dueLabel"),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = UiDefaults.CardShape
    ) {
        Column(modifier = Modifier.padding(UiDefaults.CardInnerPadding)) {
            Text(
                text = earTag,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = buildString {
                    append("Withdrawal ends: $endDate")
                    if (!product.isNullOrBlank()) append(" · $product")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = dueLabel,
                style = MaterialTheme.typography.labelLarge,
                color = statusColor
            )
        }
    }
}

@Composable
private fun WeaningWeightDueCard(
    earTag: String,
    weaningDueDate: java.time.LocalDate,
    daysUntilDue: Long,
    onClick: () -> Unit = {}
) {
    val statusColor = when {
        daysUntilDue < 0 -> MaterialTheme.colorScheme.error
        daysUntilDue <= 7 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val dueLabel = when {
        daysUntilDue < 0 -> "Overdue by ${-daysUntilDue} days"
        daysUntilDue == 0L -> "Due today"
        else -> "$daysUntilDue days until weaning weight due"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, role = Role.Button, onClickLabel = "Open $earTag, $dueLabel"),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = UiDefaults.CardShape
    ) {
        Column(modifier = Modifier.padding(UiDefaults.CardInnerPadding)) {
            Text(
                text = earTag,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Weaning weight due: $weaningDueDate",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = dueLabel,
                style = MaterialTheme.typography.labelLarge,
                color = statusColor
            )
        }
    }
}

@Composable
private fun GestationCard(
    event: BreedingEvent,
    damEarTag: String,
    dueDate: java.time.LocalDate,
    daysUntilDue: Long,
    onClick: () -> Unit = {}
) {
    val statusColor = when {
        daysUntilDue < 0 -> MaterialTheme.colorScheme.error
        daysUntilDue <= 14 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val dueLabel = when {
        daysUntilDue < 0 -> "Overdue by ${-daysUntilDue} days"
        daysUntilDue == 0L -> "Due today"
        else -> "$daysUntilDue days to calving"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, role = Role.Button, onClickLabel = "Open $damEarTag, $dueLabel"),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = UiDefaults.CardShape
    ) {
        Column(modifier = Modifier.padding(UiDefaults.CardInnerPadding)) {
            Text(
                text = damEarTag,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Service date: ${event.serviceDate} · Due: $dueDate" +
                    (event.pregnancyCheckResult?.let { " · Check: ${it.name.replace('_', ' ')}" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = dueLabel,
                style = MaterialTheme.typography.labelLarge,
                color = statusColor
            )
        }
    }
}
