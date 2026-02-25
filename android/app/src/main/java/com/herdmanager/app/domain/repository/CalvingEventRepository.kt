package com.herdmanager.app.domain.repository

import com.herdmanager.app.domain.model.CalvingEvent
import kotlinx.coroutines.flow.Flow

interface CalvingEventRepository {
    fun observeCalvedBreedingEventIds(): Flow<List<String>>
    fun observeCalvingEventsByDam(damId: String): Flow<List<CalvingEvent>>
    suspend fun getByBreedingEvent(breedingEventId: String): CalvingEvent?
    suspend fun getAllCalvingEvents(): List<CalvingEvent>
    suspend fun insertCalvingEvent(event: CalvingEvent)
}
