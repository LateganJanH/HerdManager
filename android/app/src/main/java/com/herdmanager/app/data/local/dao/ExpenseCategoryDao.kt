package com.herdmanager.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.herdmanager.app.data.local.entity.ExpenseCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseCategoryDao {
    @Query("SELECT * FROM expense_categories WHERE farmId = :farmId ORDER BY sortOrder ASC, name ASC")
    fun observeByFarm(farmId: String): Flow<List<ExpenseCategoryEntity>>

    @Query("SELECT * FROM expense_categories WHERE id = :id")
    suspend fun getById(id: String): ExpenseCategoryEntity?

    @Query("SELECT * FROM expense_categories ORDER BY sortOrder ASC, name ASC")
    suspend fun getAll(): List<ExpenseCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExpenseCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ExpenseCategoryEntity>)

    @Query("DELETE FROM expense_categories WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM expense_categories")
    suspend fun deleteAll()
}
