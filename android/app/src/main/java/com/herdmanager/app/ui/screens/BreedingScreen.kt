package com.herdmanager.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PregnantWoman
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import com.herdmanager.app.domain.model.BreedingEvent

private enum class AlertsFilter(val label: String) {
    ALL("All"),
    CALVING("Calving"),
    PREGNANCY_CHECK("Pregnancy check"),
    WITHDRAWAL("Withdrawal")
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
    var filter by remember { mutableStateOf(AlertsFilter.ALL) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val showCalving = filter == AlertsFilter.ALL || filter == AlertsFilter.CALVING
    val showPregnancyCheck = filter == AlertsFilter.ALL || filter == AlertsFilter.PREGNANCY_CHECK
    val showWithdrawal = filter == AlertsFilter.ALL || filter == AlertsFilter.WITHDRAWAL
    val hasAny = calvings.isNotEmpty() || pregnancyChecks.isNotEmpty() || withdrawals.isNotEmpty()

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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Calving due, pregnancy check and withdrawal-period reminders",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AlertsFilter.entries.forEach { f ->
                        FilterChip(
                            selected = filter == f,
                            onClick = { filter = f },
                            label = { Text(f.label) }
                        )
                    }
                }
            }

            if (!hasAny) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PregnantWoman,
                        contentDescription = "No alerts",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No calving, pregnancy check or withdrawal alerts",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Open an animal to record service",
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
                                    Text(
                                        text = "Calving due",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
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
                                    Text(
                                        text = "Pregnancy check due",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
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
                                    Text(
                                        text = "Withdrawal ends",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
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
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
