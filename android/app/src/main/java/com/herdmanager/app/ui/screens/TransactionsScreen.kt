package com.herdmanager.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.herdmanager.app.domain.model.Transaction
import com.herdmanager.app.domain.model.TransactionType
import com.herdmanager.app.ui.components.HorizontalFilterChips
import com.herdmanager.app.ui.components.SyncStatusStrip
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigateBack: () -> Unit,
    onAddTransaction: (TransactionType) -> Unit,
    onEditTransaction: (String) -> Unit,
    onManageExpenseCategories: () -> Unit,
    onAnimalClick: (String) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedCategoryFilterId by remember { mutableStateOf<String?>(null) }
    val tabTitles = listOf("Sales", "Purchases", "Expenses")
    val types = listOf(TransactionType.SALE, TransactionType.PURCHASE, TransactionType.EXPENSE)
    val type = types[selectedTabIndex]

    val sales by viewModel.sales.collectAsState(initial = emptyList())
    val purchases by viewModel.purchases.collectAsState(initial = emptyList())
    val expenses by viewModel.expenses.collectAsState(initial = emptyList())
    val categories by viewModel.expenseCategories.collectAsState(initial = emptyList())
    val lists = listOf(sales, purchases, expenses)
    val rawList = lists[selectedTabIndex]
    val currentList =
        if (type == TransactionType.EXPENSE && selectedCategoryFilterId != null) {
            rawList.filter { it.categoryId == selectedCategoryFilterId }
        } else {
            rawList
        }

    val totalsSales by viewModel.totalsSales.collectAsState(initial = TransactionTotals(0, 0, 0))
    val totalsPurchases by viewModel.totalsPurchases.collectAsState(initial = TransactionTotals(0, 0, 0))
    val totalsExpenses by viewModel.totalsExpenses.collectAsState(initial = TransactionTotals(0, 0, 0))
    val totalsList = listOf(totalsSales, totalsPurchases, totalsExpenses)
    val totals = totalsList[selectedTabIndex]

    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    val currencyCode by viewModel.currencyCode.collectAsState(initial = com.herdmanager.app.domain.model.FarmSettings.DEFAULT_CURRENCY_CODE)
    val lastSyncedAt by viewModel.lastSyncedAt.collectAsState(initial = null)
    val isSyncing by viewModel.isSyncing.collectAsState(initial = false)
    val syncError by viewModel.syncError.collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddTransaction(type) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add transaction")
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isSyncing,
            onRefresh = { viewModel.syncNow() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag("transactions_screen")
            ) {
                SyncStatusStrip(
                    lastSyncedAt = lastSyncedAt,
                    isSyncing = isSyncing,
                    syncError = syncError?.message,
                    formatLastSynced = { viewModel.formatLastSynced(it) },
                    onSync = { viewModel.syncNow() },
                    onDismissError = { viewModel.clearSyncError() }
                )
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }
                TotalsCard(
                    totalThisMonth = totals.totalThisMonth,
                    totalThisYear = totals.totalThisYear,
                    grandTotal = totals.grandTotal,
                    currencyCode = currencyCode,
                    formatCents = { viewModel.formatCentsToCurrency(it, currencyCode) }
                )
                if (type == TransactionType.EXPENSE) {
                    // Category filter chips
                    val categoryOptions = listOf("All" to (selectedCategoryFilterId == null)) +
                        categories.map { cat ->
                            cat.name to (cat.id == selectedCategoryFilterId)
                        }
                    if (categoryOptions.isNotEmpty()) {
                        HorizontalFilterChips(
                            options = categoryOptions,
                            onOptionSelected = { index ->
                                selectedCategoryFilterId = if (index == 0) null else categories.getOrNull(index - 1)?.id
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onManageExpenseCategories) {
                            Text("Manage categories")
                        }
                    }
                }
                transactionToDelete?.let { pending ->
                    AlertDialog(
                        onDismissRequest = { transactionToDelete = null },
                        title = { Text("Delete transaction") },
                        text = { Text("This action cannot be undone.") },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.deleteTransaction(pending.id)
                                transactionToDelete = null
                            }) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { transactionToDelete = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                if (currentList.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No transactions yet", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tap + to add your first ${type.name.lowercase()} transaction.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(currentList, key = { it.id }) { t ->
                            val categoryName: String? =
                                if (t.type == TransactionType.EXPENSE) {
                                    categories.find { it.id == t.categoryId }?.name
                                } else null

                            TransactionRow(
                                transaction = t,
                                categoryName = categoryName,
                                formatCents = { viewModel.formatCentsToCurrency(it, currencyCode) },
                                onClick = { onEditTransaction(t.id) },
                                onLongClick = { transactionToDelete = t },
                                onAnimalClick = t.animalId?.let { { onAnimalClick(it) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalsCard(
    totalThisMonth: Long,
    totalThisYear: Long,
    grandTotal: Long,
    currencyCode: String,
    formatCents: (Long) -> String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("This month", style = MaterialTheme.typography.labelMedium)
            Text(
                formatCents(totalThisMonth),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("This year", style = MaterialTheme.typography.labelMedium)
            Text(
                formatCents(totalThisYear),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Grand total", style = MaterialTheme.typography.labelMedium)
            Text(
                formatCents(grandTotal),
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionRow(
    transaction: Transaction,
    categoryName: String?,
    formatCents: (Long) -> String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAnimalClick: (() -> Unit)?
) {
    val dateStr = DateTimeFormatter.ofPattern("MMM d, yyyy")
        .format(TransactionsViewModel.epochDayToLocalDate(transaction.dateEpochDay))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    formatCents(transaction.amountCents),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(dateStr, style = MaterialTheme.typography.bodySmall)
                when (transaction.type) {
                    TransactionType.SALE, TransactionType.PURCHASE ->
                        transaction.displayContactName?.let { name ->
                            Text(name, style = MaterialTheme.typography.bodySmall)
                        }
                    TransactionType.EXPENSE -> {
                        categoryName?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                        transaction.description?.takeIf { it.isNotBlank() }?.let { desc ->
                            Text(desc, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                    }
                }
            }
            if (onAnimalClick != null) {
                IconButton(onClick = onAnimalClick) {
                    Icon(Icons.Default.Receipt, contentDescription = "View animal")
                }
            }
        }
    }
}

