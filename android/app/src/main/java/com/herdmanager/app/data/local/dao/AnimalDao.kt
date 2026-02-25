package com.herdmanager.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.herdmanager.app.data.local.entity.AnimalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimalDao {
    @Query("SELECT * FROM animals WHERE farmId = :farmId ORDER BY earTagNumber ASC")
    fun observeByFarm(farmId: String): Flow<List<AnimalEntity>>

    @Query("SELECT * FROM animals WHERE farmId = :farmId AND (:herdId IS NULL OR currentHerdId = :herdId OR (:herdId = 'unassigned' AND currentHerdId IS NULL)) ORDER BY earTagNumber ASC")
    fun observeByFarmAndHerd(farmId: String, herdId: String?): Flow<List<AnimalEntity>>

    @Query("SELECT * FROM animals WHERE id = :id")
    suspend fun getById(id: String): AnimalEntity?

    @Query("SELECT * FROM animals WHERE earTagNumber = :earTagNumber AND farmId = :farmId LIMIT 1")
    suspend fun getByEarTagAndFarm(earTagNumber: String, farmId: String): AnimalEntity?

    @Query("SELECT * FROM animals ORDER BY earTagNumber ASC")
    suspend fun getAll(): List<AnimalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(animal: AnimalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(animals: List<AnimalEntity>)

    @Query("UPDATE animals SET currentHerdId = null, updatedAt = :updatedAt WHERE currentHerdId = :herdId")
    suspend fun clearCurrentHerdForHerd(herdId: String, updatedAt: Long)

    @Query("DELETE FROM animals WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM animals")
    suspend fun deleteAll()
}
