package com.herdmanager.app.domain.repository

import com.herdmanager.app.domain.model.ExpenseCategory
import kotlinx.coroutines.flow.Flow

interface ExpenseCategoryRepository {
    fun observeCategoriesByFarm(farmId: String): Flow<List<ExpenseCategory>>
    suspend fun getCategoryById(id: String): ExpenseCategory?
    suspend fun insertCategory(category: ExpenseCategory)
    suspend fun deleteCategory(id: String)
}
