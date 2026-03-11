package com.herdmanager.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.herdmanager.app.data.local.entity.FarmTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmTaskDao {

    @Query(
        """
        SELECT * FROM farm_tasks
        ORDER BY
          CASE WHEN status IN ('PENDING', 'IN_PROGRESS') THEN 0 ELSE 1 END ASC,
          COALESCE(dueDateEpochDay, 9223372036854775807) ASC,
          createdAt ASC
        """
    )
    fun observeAll(): Flow<List<FarmTaskEntity>>

    @Query(
        """
        SELECT * FROM farm_tasks
        WHERE status = :status
        ORDER BY COALESCE(dueDateEpochDay, 9223372036854775807) ASC, createdAt ASC
        """
    )
    fun observeByStatus(status: String): Flow<List<FarmTaskEntity>>

    @Query(
        """
        SELECT * FROM farm_tasks
        WHERE dueDateEpochDay IS NOT NULL
          AND dueDateEpochDay BETWEEN :startEpochDay AND :endEpochDay
        ORDER BY dueDateEpochDay ASC, createdAt ASC
        """
    )
    fun observeDueBetween(startEpochDay: Long, endEpochDay: Long): Flow<List<FarmTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: FarmTaskEntity)

    @Update
    suspend fun update(task: FarmTaskEntity)

    @Query("UPDATE farm_tasks SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long)

    @Query("DELETE FROM farm_tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM farm_tasks")
    suspend fun getAll(): List<FarmTaskEntity>

    @Query("SELECT * FROM farm_tasks WHERE id = :id")
    suspend fun getById(id: String): FarmTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<FarmTaskEntity>)

    @Query("DELETE FROM farm_tasks")
    suspend fun deleteAll()
}

