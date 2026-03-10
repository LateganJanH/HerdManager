package com.herdmanager.app.ui.screens

import com.herdmanager.app.domain.model.Animal
import com.herdmanager.app.domain.model.AnimalStatus
import com.herdmanager.app.domain.model.ExpenseCategory
import com.herdmanager.app.domain.model.FarmSettings
import com.herdmanager.app.domain.model.Transaction
import com.herdmanager.app.domain.model.TransactionType
import com.herdmanager.app.domain.repository.AnimalRepository
import com.herdmanager.app.domain.repository.ExpenseCategoryRepository
import com.herdmanager.app.domain.repository.FarmSettingsRepository
import com.herdmanager.app.domain.repository.SyncRepository
import com.herdmanager.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

private class FakeTransactionRepository : TransactionRepository {
    private val stored = mutableMapOf<String, Transaction>()
    private val transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())

    override fun observeTransactionsByFarm(farmId: String): Flow<List<Transaction>> = transactionsFlow

    override fun observeTransactionsByFarmAndType(farmId: String, type: TransactionType): Flow<List<Transaction>> =
        transactionsFlow

    override fun observeTransactionsByAnimal(animalId: String): Flow<List<Transaction>> = transactionsFlow

    override suspend fun getTransactionById(id: String): Transaction? = stored[id]

    override suspend fun insertTransaction(transaction: Transaction) {
        stored[transaction.id] = transaction
        transactionsFlow.value = stored.values.toList()
    }

    override suspend fun deleteTransaction(id: String) {
        stored.remove(id)
        transactionsFlow.value = stored.values.toList()
    }
}

private class FakeAnimalRepository : AnimalRepository {
    private val animals = mutableMapOf<String, Animal>()

    override fun observeAnimalsByFarm(farmId: String) = MutableStateFlow(animals.values.toList())

    override fun observeAnimalsByFarmAndHerd(farmId: String, herdId: String?): Flow<List<Animal>> =
        MutableStateFlow(animals.values.toList())

    override suspend fun getAnimalById(id: String): Animal? = animals[id]

    override suspend fun insertAnimal(animal: Animal) {
        animals[animal.id] = animal
    }

    override suspend fun deleteAnimal(id: String) {
        animals.remove(id)
    }
}

private class FakeExpenseCategoryRepository : ExpenseCategoryRepository {
    private val categoriesFlow = MutableStateFlow<List<ExpenseCategory>>(emptyList())

    override fun observeCategoriesByFarm(farmId: String): Flow<List<ExpenseCategory>> = categoriesFlow

    override suspend fun getCategoryById(id: String): ExpenseCategory? =
        categoriesFlow.value.firstOrNull { it.id == id }

    override suspend fun insertCategory(category: ExpenseCategory) {
        categoriesFlow.value = categoriesFlow.value + category
    }

    override suspend fun deleteCategory(id: String) {
        categoriesFlow.value = categoriesFlow.value.filterNot { it.id == id }
    }
}

private class FakeFarmSettingsRepository : FarmSettingsRepository {
    private val flow = MutableStateFlow(FarmSettings(id = FarmSettings.DEFAULT_FARM_ID))
    override fun farmSettings() = flow
    override suspend fun updateFarmSettings(settings: FarmSettings) {
        flow.value = settings
    }
}

private class FakeSyncRepository : SyncRepository {
    override fun lastSyncedAt(): Flow<Instant?> = MutableStateFlow<Instant?>(null)
    override suspend fun syncNow() = Result.success(Unit)
}

class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        super.starting(description)
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadTransaction_populatesSelectedTransaction() = runTest {
        val transactionRepository = FakeTransactionRepository()
        val target = Transaction(
            id = "t1",
            type = TransactionType.EXPENSE,
            amountCents = 1000,
            dateEpochDay = 0,
            farmId = FarmSettings.DEFAULT_FARM_ID,
            notes = null,
            createdAt = 1L,
            updatedAt = 1L
        )
        transactionRepository.insertTransaction(target)

        val vm = TransactionsViewModel(
            transactionRepository = transactionRepository,
            expenseCategoryRepository = FakeExpenseCategoryRepository(),
            animalRepository = FakeAnimalRepository(),
            farmSettingsRepository = FakeFarmSettingsRepository(),
            syncRepository = FakeSyncRepository()
        )

        vm.loadTransaction("t1")

        val loaded = vm.selectedTransaction.first { it != null }
        assertNotNull(loaded)
    }

    @Test
    fun saveTransaction_withSetAnimalSold_updatesAnimalStatus() = runTest {
        val transactionRepository = FakeTransactionRepository()
        val animalRepository = FakeAnimalRepository()
        val animal = Animal(
            id = "a1",
            earTagNumber = "123",
            sex = com.herdmanager.app.domain.model.Sex.FEMALE,
            breed = "Breed",
            dateOfBirth = LocalDate.of(2020, 1, 1),
            farmId = FarmSettings.DEFAULT_FARM_ID,
            status = AnimalStatus.ACTIVE
        )
        animalRepository.insertAnimal(animal)

        val vm = TransactionsViewModel(
            transactionRepository = transactionRepository,
            expenseCategoryRepository = FakeExpenseCategoryRepository(),
            animalRepository = animalRepository,
            farmSettingsRepository = FakeFarmSettingsRepository(),
            syncRepository = FakeSyncRepository()
        )

        val sale = Transaction(
            id = "sale1",
            type = TransactionType.SALE,
            amountCents = 5000,
            dateEpochDay = 0,
            farmId = FarmSettings.DEFAULT_FARM_ID,
            notes = null,
            createdAt = 1L,
            updatedAt = 1L,
            animalId = "a1"
        )

        vm.saveTransaction(sale, setAnimalSold = true)

        val updated = animalRepository.getAnimalById("a1")
        assertNotNull(updated)
        assertEquals(AnimalStatus.SOLD, updated?.status)
    }
}

