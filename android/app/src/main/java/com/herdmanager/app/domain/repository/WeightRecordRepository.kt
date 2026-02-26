package com.herdmanager.app.domain.repository

import com.herdmanager.app.domain.model.WeightRecord
import kotlinx.coroutines.flow.Flow

interface WeightRecordRepository {
    fun observeWeightRecordsByAnimal(animalId: String): Flow<List<WeightRecord>>
    /** All weight records (e.g. for weaning-due alerts). */
    fun observeAllWeightRecords(): Flow<List<WeightRecord>>
    suspend fun insertWeightRecord(record: WeightRecord)
    suspend fun updateWeightRecord(record: WeightRecord)
    suspend fun deleteWeightRecord(id: String)
}
