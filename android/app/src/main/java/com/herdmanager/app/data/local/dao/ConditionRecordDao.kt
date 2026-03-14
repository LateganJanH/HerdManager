package com.herdmanager.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.herdmanager.app.data.local.entity.ConditionRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConditionRecordDao {

    @Query("SELECT * FROM condition_records WHERE animalId = :animalId ORDER BY dateEpochDay DESC")
    fun observeByAnimal(animalId: String): Flow<List<ConditionRecordEntity>>

    @Query("SELECT * FROM condition_records ORDER BY dateEpochDay DESC")
    fun observeAll(): Flow<List<ConditionRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ConditionRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<ConditionRecordEntity>)

    @Query("DELETE FROM condition_records WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM condition_records ORDER BY dateEpochDay DESC")
    suspend fun getAll(): List<ConditionRecordEntity>

    @Query("DELETE FROM condition_records")
    suspend fun deleteAll()
}

