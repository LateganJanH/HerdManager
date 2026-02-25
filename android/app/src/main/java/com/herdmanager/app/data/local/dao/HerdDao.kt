package com.herdmanager.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.herdmanager.app.data.local.entity.HerdEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HerdDao {
    @Query("SELECT * FROM herds WHERE farmId = :farmId ORDER BY sortOrder ASC, name ASC")
    fun observeByFarm(farmId: String): Flow<List<HerdEntity>>

    @Query("SELECT * FROM herds WHERE farmId = :farmId ORDER BY sortOrder ASC, name ASC")
    suspend fun getByFarm(farmId: String): List<HerdEntity>

    @Query("SELECT * FROM herds WHERE id = :id")
    suspend fun getById(id: String): HerdEntity?

    @Query("SELECT * FROM herds ORDER BY sortOrder ASC, name ASC")
    suspend fun getAll(): List<HerdEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(herd: HerdEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(herds: List<HerdEntity>)

    @Query("DELETE FROM herds WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM herds WHERE farmId = :farmId")
    suspend fun deleteByFarm(farmId: String)

    @Query("DELETE FROM herds")
    suspend fun deleteAll()
}
