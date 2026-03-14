package com.herdmanager.app.domain.repository

import com.herdmanager.app.domain.model.ConditionRecord
import kotlinx.coroutines.flow.Flow

interface ConditionRecordRepository {
    fun observeByAnimal(animalId: String): Flow<List<ConditionRecord>>
    fun observeAll(): Flow<List<ConditionRecord>>
    suspend fun insert(record: ConditionRecord)
    suspend fun update(record: ConditionRecord)
    suspend fun delete(id: String)
}

