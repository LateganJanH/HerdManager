package com.herdmanager.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.herdmanager.app.data.local.entity.HerdAssignmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HerdAssignmentDao {
    @Query("SELECT * FROM herd_assignments WHERE animalId = :animalId ORDER BY assignedAt DESC")
    fun observeByAnimal(animalId: String): Flow<List<HerdAssignmentEntity>>

    @Query("SELECT * FROM herd_assignments WHERE herdId = :herdId AND removedAt IS NULL")
    fun observeCurrentByHerd(herdId: String): Flow<List<HerdAssignmentEntity>>

    @Query("SELECT * FROM herd_assignments WHERE animalId = :animalId ORDER BY assignedAt DESC")
    suspend fun getByAnimal(animalId: String): List<HerdAssignmentEntity>

    @Query("SELECT * FROM herd_assignments WHERE herdId = :herdId ORDER BY assignedAt DESC")
    suspend fun getByHerd(herdId: String): List<HerdAssignmentEntity>

    @Query("SELECT * FROM herd_assignments ORDER BY assignedAt DESC")
    suspend fun getAll(): List<HerdAssignmentEntity>

    @Query("SELECT * FROM herd_assignments WHERE animalId = :animalId AND removedAt IS NULL LIMIT 1")
    suspend fun getCurrentByAnimal(animalId: String): HerdAssignmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(assignment: HerdAssignmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(assignments: List<HerdAssignmentEntity>)

    @Query("DELETE FROM herd_assignments WHERE animalId = :animalId")
    suspend fun deleteByAnimal(animalId: String)

    @Query("DELETE FROM herd_assignments WHERE herdId = :herdId")
    suspend fun deleteByHerd(herdId: String)

    @Query("DELETE FROM herd_assignments")
    suspend fun deleteAll()
}
