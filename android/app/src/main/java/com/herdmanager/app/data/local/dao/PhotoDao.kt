package com.herdmanager.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.herdmanager.app.data.local.entity.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE animalId = :animalId ORDER BY capturedAt DESC")
    fun observeByAnimal(animalId: String): Flow<List<PhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: PhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<PhotoEntity>)

    @Delete
    suspend fun delete(photo: PhotoEntity)

    @Query("SELECT * FROM photos ORDER BY capturedAt DESC")
    suspend fun getAll(): List<PhotoEntity>

    @Query("SELECT * FROM photos ORDER BY capturedAt DESC")
    fun observeAll(): Flow<List<PhotoEntity>>

    @Query("DELETE FROM photos")
    suspend fun deleteAll()
}
