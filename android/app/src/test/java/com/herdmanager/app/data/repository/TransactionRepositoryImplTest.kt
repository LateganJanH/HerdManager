package com.herdmanager.app.data.repository

import com.herdmanager.app.data.local.dao.TransactionDao
import com.herdmanager.app.data.local.entity.TransactionEntity
import com.herdmanager.app.domain.model.Transaction
import com.herdmanager.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeTransactionDao : TransactionDao {
    private val entities = mutableMapOf<String, TransactionEntity>()
    private val flow = MutableStateFlow<List<TransactionEntity>>(emptyList())

    override fun observeByFarm(farmId: String): Flow<List<TransactionEntity>> = flow

    override fun observeByFarmAndType(farmId: String, type: String): Flow<List<TransactionEntity>> = flow

    override fun observeByAnimal(animalId: String): Flow<List<TransactionEntity>> = flow

    override suspend fun getAll(): List<TransactionEntity> = entities.values.toList()

    override suspend fun getById(id: String): TransactionEntity? = entities[id]

    override suspend fun insert(entity: TransactionEntity) {
        entities[entity.id] = entity
        flow.value = entities.values.toList()
    }

    override suspend fun insertAll(entities: List<TransactionEntity>) {
        entities.forEach { this.entities[it.id] = it }
        flow.value = this.entities.values.toList()
    }

    override suspend fun deleteById(id: String) {
        entities.remove(id)
        flow.value = entities.values.toList()
    }

    override suspend fun deleteAll() {
        entities.clear()
        flow.value = emptyList()
    }
}

class TransactionRepositoryImplTest {

    @Test
    fun insertAndGetTransaction_preservesAllFields() = runBlocking {
        val dao = FakeTransactionDao()
        val repo = TransactionRepositoryImpl(dao)

        val transaction = Transaction(
            id = "t1",
            type = TransactionType.SALE,
            amountCents = 12345,
            dateEpochDay = 20000,
            farmId = "farm1",
            notes = "Note",
            createdAt = 1L,
            updatedAt = 1L,
            weightKg = 250.5,
            pricePerKgCents = 5000,
            animalId = "animal1",
            contactName = "Buyer",
            contactPhone = "123",
            contactEmail = "buyer@example.com",
            categoryId = "cat1",
            description = "Desc"
        )

        repo.insertTransaction(transaction)

        val roundTripped = repo.getTransactionById("t1")
        requireNotNull(roundTripped)

        assertEquals(transaction.id, roundTripped.id)
        assertEquals(transaction.type, roundTripped.type)
        assertEquals(transaction.amountCents, roundTripped.amountCents)
        assertEquals(transaction.dateEpochDay, roundTripped.dateEpochDay)
        assertEquals(transaction.farmId, roundTripped.farmId)
        assertEquals(transaction.notes, roundTripped.notes)
        assertEquals(transaction.weightKg, roundTripped.weightKg)
        assertEquals(transaction.pricePerKgCents, roundTripped.pricePerKgCents)
        assertEquals(transaction.animalId, roundTripped.animalId)
        assertEquals(transaction.contactName, roundTripped.contactName)
        assertEquals(transaction.contactPhone, roundTripped.contactPhone)
        assertEquals(transaction.contactEmail, roundTripped.contactEmail)
        assertEquals(transaction.categoryId, roundTripped.categoryId)
        assertEquals(transaction.description, roundTripped.description)
    }

    @Test
    fun observeTransactionsByFarm_emitsInsertedTransactions() = runBlocking {
        val dao = FakeTransactionDao()
        val repo = TransactionRepositoryImpl(dao)

        val transaction = Transaction(
            id = "t2",
            type = TransactionType.EXPENSE,
            amountCents = 5000,
            dateEpochDay = 20001,
            farmId = "farm1",
            notes = null,
            createdAt = 1L,
            updatedAt = 1L,
            categoryId = "feed",
            description = "Feed"
        )

        repo.insertTransaction(transaction)

        val list = repo.observeTransactionsByFarm("farm1").first()
        assertEquals(1, list.size)
        assertEquals("t2", list.first().id)
    }
}

