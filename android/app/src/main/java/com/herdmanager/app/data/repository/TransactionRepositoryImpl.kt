package com.herdmanager.app.data.repository

import com.herdmanager.app.data.local.dao.TransactionDao
import com.herdmanager.app.data.local.entity.TransactionEntity
import com.herdmanager.app.domain.model.Transaction
import com.herdmanager.app.domain.model.TransactionType
import com.herdmanager.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val dao: TransactionDao
) : TransactionRepository {

    override fun observeTransactionsByFarm(farmId: String): Flow<List<Transaction>> =
        dao.observeByFarm(farmId).map { it.map(TransactionEntity::toDomain) }

    override fun observeTransactionsByFarmAndType(farmId: String, type: TransactionType): Flow<List<Transaction>> =
        dao.observeByFarmAndType(farmId, type.name).map { it.map(TransactionEntity::toDomain) }

    override fun observeTransactionsByAnimal(animalId: String): Flow<List<Transaction>> =
        dao.observeByAnimal(animalId).map { it.map(TransactionEntity::toDomain) }

    override suspend fun getTransactionById(id: String): Transaction? =
        dao.getById(id)?.toDomain()

    override suspend fun insertTransaction(transaction: Transaction) {
        val now = System.currentTimeMillis()
        val existing = dao.getById(transaction.id)
        val entity = transaction.toEntity(
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        dao.insert(entity)
    }

    override suspend fun deleteTransaction(id: String) {
        dao.deleteById(id)
    }
}

private fun TransactionEntity.toDomain() = Transaction(
    id = id,
    type = TransactionType.valueOf(type),
    amountCents = amountCents,
    dateEpochDay = dateEpochDay,
    farmId = farmId,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    weightKg = weightKg,
    pricePerKgCents = pricePerKgCents,
    animalId = animalId,
    contactName = contactName,
    contactPhone = contactPhone,
    contactEmail = contactEmail,
    categoryId = categoryId,
    description = description
)

private fun Transaction.toEntity(createdAt: Long, updatedAt: Long) = TransactionEntity(
    id = id,
    type = type.name,
    amountCents = amountCents,
    dateEpochDay = dateEpochDay,
    farmId = farmId,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    weightKg = weightKg,
    pricePerKgCents = pricePerKgCents,
    animalId = animalId,
    contactName = contactName,
    contactPhone = contactPhone,
    contactEmail = contactEmail,
    categoryId = categoryId,
    description = description
)
