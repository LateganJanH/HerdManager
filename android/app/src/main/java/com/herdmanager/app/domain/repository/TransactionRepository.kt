package com.herdmanager.app.domain.repository

import com.herdmanager.app.domain.model.Transaction
import com.herdmanager.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeTransactionsByFarm(farmId: String): Flow<List<Transaction>>
    fun observeTransactionsByFarmAndType(farmId: String, type: TransactionType): Flow<List<Transaction>>
    fun observeTransactionsByAnimal(animalId: String): Flow<List<Transaction>>
    suspend fun getTransactionById(id: String): Transaction?
    suspend fun insertTransaction(transaction: Transaction)
    suspend fun deleteTransaction(id: String)
}
