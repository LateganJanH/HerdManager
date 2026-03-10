package com.herdmanager.app.data.repository

import com.herdmanager.app.data.local.dao.ExpenseCategoryDao
import com.herdmanager.app.data.local.entity.ExpenseCategoryEntity
import com.herdmanager.app.domain.model.ExpenseCategory
import com.herdmanager.app.domain.repository.ExpenseCategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExpenseCategoryRepositoryImpl(
    private val dao: ExpenseCategoryDao
) : ExpenseCategoryRepository {

    override fun observeCategoriesByFarm(farmId: String): Flow<List<ExpenseCategory>> =
        dao.observeByFarm(farmId).map { it.map(ExpenseCategoryEntity::toDomain) }

    override suspend fun getCategoryById(id: String): ExpenseCategory? =
        dao.getById(id)?.toDomain()

    override suspend fun insertCategory(category: ExpenseCategory) {
        val now = System.currentTimeMillis()
        val existing = dao.getById(category.id)
        val entity = category.toEntity(
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        dao.insert(entity)
    }

    override suspend fun deleteCategory(id: String) {
        dao.deleteById(id)
    }
}

private fun ExpenseCategoryEntity.toDomain() = ExpenseCategory(
    id = id,
    name = name,
    farmId = farmId,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun ExpenseCategory.toEntity(createdAt: Long, updatedAt: Long) = ExpenseCategoryEntity(
    id = id,
    name = name,
    farmId = farmId,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt
)
