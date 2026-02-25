package com.herdmanager.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.herdmanager.app.data.local.entity.BreedingEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BreedingEventDao {
    @Query("SELECT * FROM breeding_events WHERE animalId = :animalId ORDER BY serviceDate DESC")
    fun observeByAnimal(animalId: String): Flow<List<BreedingEventEntity>>

    @Query("SELECT * FROM breeding_events ORDER BY serviceDate DESC")
    fun observeAll(): Flow<List<BreedingEventEntity>>

    @Query("SELECT * FROM breeding_events WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BreedingEventEntity?

    @Query("SELECT * FROM breeding_events ORDER BY serviceDate DESC")
    suspend fun getAll(): List<BreedingEventEntity>

    @Query("DELETE FROM breeding_events")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: BreedingEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<BreedingEventEntity>)
}
