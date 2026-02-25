package com.herdmanager.app.domain.repository

import com.herdmanager.app.domain.model.WeightRecord
import kotlinx.coroutines.flow.Flow

interface WeightRecordRepository {
    fun observeWeightRecordsByAnimal(animalId: String): Flow<List<WeightRecord>>
    suspend fun insertWeightRecord(record: WeightRecord)
    suspend fun deleteWeightRecord(id: String)
}
