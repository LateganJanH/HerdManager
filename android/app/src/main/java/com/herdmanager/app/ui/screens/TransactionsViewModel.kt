package com.herdmanager.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herdmanager.app.domain.model.Animal
import com.herdmanager.app.domain.model.AnimalStatus
import com.herdmanager.app.domain.model.isInCurrentHerd
import com.herdmanager.app.domain.model.ExpenseCategory
import com.herdmanager.app.domain.model.FarmSettings
import com.herdmanager.app.domain.model.Transaction
import com.herdmanager.app.domain.model.TransactionType
import com.herdmanager.app.domain.repository.AnimalRepository
import com.herdmanager.app.domain.repository.ExpenseCategoryRepository
import com.herdmanager.app.domain.repository.FarmSettingsRepository
import com.herdmanager.app.domain.repository.SyncRepository
import com.herdmanager.app.domain.repository.TransactionRepository
import com.herdmanager.app.ui.util.CurrencyFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class TransactionTotals(
    val totalThisMonth: Long,
    val totalThisYear: Long,
    val grandTotal: Long
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val expenseCategoryRepository: ExpenseCategoryRepository,
    private val animalRepository: AnimalRepository,
    private val farmSettingsRepository: FarmSettingsRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val farmId: String
        get() = kotlinx.coroutines.runBlocking { farmSettingsRepository.farmSettings().first().id.ifBlank { FarmSettings.DEFAULT_FARM_ID } }

    val transactions: StateFlow<List<Transaction>> = transactionRepository
        .observeTransactionsByFarm(FarmSettings.DEFAULT_FARM_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenseCategories: StateFlow<List<ExpenseCategory>> = expenseCategoryRepository
        .observeCategoriesByFarm(FarmSettings.DEFAULT_FARM_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val animals: StateFlow<List<Animal>> = animalRepository
        .observeAnimalsByFarm(FarmSettings.DEFAULT_FARM_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastSyncedAt = syncRepository.lastSyncedAt()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    private val _syncError = MutableStateFlow<Throwable?>(null)
    val syncError: StateFlow<Throwable?> = _syncError.asStateFlow()

    private val _selectedTransaction = MutableStateFlow<Transaction?>(null)
    val selectedTransaction: StateFlow<Transaction?> = _selectedTransaction.asStateFlow()

    fun loadTransaction(id: String) {
        viewModelScope.launch {
            _selectedTransaction.value = transactionRepository.getTransactionById(id)
        }
    }

    fun clearSelectedTransaction() { _selectedTransaction.value = null }

    val sales: StateFlow<List<Transaction>> = transactionRepository
        .observeTransactionsByFarmAndType(FarmSettings.DEFAULT_FARM_ID, TransactionType.SALE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchases: StateFlow<List<Transaction>> = transactionRepository
        .observeTransactionsByFarmAndType(FarmSettings.DEFAULT_FARM_ID, TransactionType.PURCHASE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<Transaction>> = transactionRepository
        .observeTransactionsByFarmAndType(FarmSettings.DEFAULT_FARM_ID, TransactionType.EXPENSE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun totalsFlowForType(type: TransactionType): StateFlow<TransactionTotals> =
        transactionRepository
            .observeTransactionsByFarmAndType(FarmSettings.DEFAULT_FARM_ID, type)
            .map { list ->
                val now = LocalDate.now()
                val thisMonthStart = now.withDayOfMonth(1).toEpochDay()
                val thisYearStart = now.withDayOfYear(1).toEpochDay()
                var totalMonth = 0L
                var totalYear = 0L
                var grand = 0L
                list.forEach { t ->
                    grand += t.amountCents
                    if (t.dateEpochDay >= thisMonthStart) totalMonth += t.amountCents
                    if (t.dateEpochDay >= thisYearStart) totalYear += t.amountCents
                }
                TransactionTotals(totalMonth, totalYear, grand)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionTotals(0, 0, 0))

    val totalsSales: StateFlow<TransactionTotals> = totalsFlowForType(TransactionType.SALE)
    val totalsPurchases: StateFlow<TransactionTotals> = totalsFlowForType(TransactionType.PURCHASE)
    val totalsExpenses: StateFlow<TransactionTotals> = totalsFlowForType(TransactionType.EXPENSE)

    fun saveTransaction(transaction: Transaction, setAnimalSold: Boolean = false) {
        viewModelScope.launch {
            transactionRepository.insertTransaction(transaction)
            if (setAnimalSold && transaction.type == TransactionType.SALE && transaction.animalId != null) {
                val animal = animalRepository.getAnimalById(transaction.animalId)
                if (animal != null && animal.isInCurrentHerd) {
                    animalRepository.insertAnimal(
                        animal.copy(status = AnimalStatus.SOLD)
                    )
                }
            }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
        }
    }

    fun saveExpenseCategory(category: ExpenseCategory) {
        viewModelScope.launch {
            expenseCategoryRepository.insertCategory(category)
        }
    }

    fun deleteExpenseCategory(id: String) {
        viewModelScope.launch {
            expenseCategoryRepository.deleteCategory(id)
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            syncRepository.syncNow()
                .onSuccess { _isSyncing.value = false }
                .onFailure { _syncError.value = it; _isSyncing.value = false }
        }
    }

    fun clearSyncError() { _syncError.value = null }

    fun formatLastSynced(instant: Instant?): String {
        if (instant == null) return "Never"
        val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm")
        return formatter.format(instant.atZone(ZoneId.systemDefault()))
    }

    /** Current currency code from farm settings (e.g. ZAR). Used for all currency display. */
    val currencyCode: StateFlow<String> = farmSettingsRepository.farmSettings()
        .map { it.currencyCode.ifBlank { FarmSettings.DEFAULT_CURRENCY_CODE } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FarmSettings.DEFAULT_CURRENCY_CODE)

    /** Format amount in cents using the farm's selected currency. */
    fun formatCentsToCurrency(cents: Long, code: String): String =
        CurrencyFormat.formatCents(cents, code)

    companion object {
        fun epochDayToLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)
    }
}
