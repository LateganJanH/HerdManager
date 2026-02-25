package com.herdmanager.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.herdmanager.app.data.local.entity.CalvingEventEntity
import kotlinx.coroutines.flow.Flow

data class CalvedBreedingEventId(val id: String)

@Dao
interface CalvingEventDao {
    @Query("SELECT * FROM calving_events WHERE damId = :damId ORDER BY actualDate DESC")
    fun observeByDam(damId: String): Flow<List<CalvingEventEntity>>

    @Query("SELECT * FROM calving_events WHERE breedingEventId = :breedingEventId ORDER BY actualDate DESC LIMIT 1")
    suspend fun getByBreedingEvent(breedingEventId: String): CalvingEventEntity?

    @Query("SELECT DISTINCT breedingEventId as id FROM calving_events")
    fun observeCalvedBreedingEventIds(): Flow<List<CalvedBreedingEventId>>

    @Query("SELECT * FROM calving_events ORDER BY actualDate DESC")
    suspend fun getAll(): List<CalvingEventEntity>

    @Query("DELETE FROM calving_events")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: CalvingEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<CalvingEventEntity>)
}
