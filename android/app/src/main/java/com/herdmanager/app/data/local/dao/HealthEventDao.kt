package com.herdmanager.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.herdmanager.app.data.local.entity.HealthEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthEventDao {
    @Query("SELECT * FROM health_events WHERE animalId = :animalId ORDER BY date DESC")
    fun observeByAnimal(animalId: String): Flow<List<HealthEventEntity>>

    @Query("SELECT * FROM health_events ORDER BY date DESC")
    fun observeAll(): Flow<List<HealthEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: HealthEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<HealthEventEntity>)

    @Query("DELETE FROM health_events WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM health_events ORDER BY date DESC")
    suspend fun getAll(): List<HealthEventEntity>

    @Query("DELETE FROM health_events")
    suspend fun deleteAll()
}
