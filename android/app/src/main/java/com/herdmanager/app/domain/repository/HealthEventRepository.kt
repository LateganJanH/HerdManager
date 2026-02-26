package com.herdmanager.app.domain.repository

import com.herdmanager.app.domain.model.HealthEvent
import kotlinx.coroutines.flow.Flow

interface HealthEventRepository {
    fun observeHealthEventsByAnimal(animalId: String): Flow<List<HealthEvent>>
    fun observeAllHealthEvents(): Flow<List<HealthEvent>>
    suspend fun insertHealthEvent(event: HealthEvent)
    suspend fun updateHealthEvent(event: HealthEvent)
    suspend fun deleteHealthEvent(id: String)
}
