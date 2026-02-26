package com.herdmanager.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.herdmanager.app.data.local.entity.WeightRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightRecordDao {
    @Query("SELECT * FROM weight_records WHERE animalId = :animalId ORDER BY date DESC")
    fun observeByAnimal(animalId: String): Flow<List<WeightRecordEntity>>

    @Query("SELECT * FROM weight_records ORDER BY date DESC")
    fun observeAll(): Flow<List<WeightRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: WeightRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<WeightRecordEntity>)

    @Query("DELETE FROM weight_records WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM weight_records ORDER BY date DESC")
    suspend fun getAll(): List<WeightRecordEntity>

    @Query("DELETE FROM weight_records")
    suspend fun deleteAll()
}
