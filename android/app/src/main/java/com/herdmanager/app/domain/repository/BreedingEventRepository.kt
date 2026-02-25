package com.herdmanager.app.domain.repository

import com.herdmanager.app.domain.model.BreedingEvent
import com.herdmanager.app.domain.model.PregnancyCheckResult
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface BreedingEventRepository {
    fun observeBreedingEventsByAnimal(animalId: String): Flow<List<BreedingEvent>>
    fun observeAllBreedingEvents(): Flow<List<BreedingEvent>>
    suspend fun getBreedingEventById(id: String): BreedingEvent?
    suspend fun insertBreedingEvent(event: BreedingEvent)
    suspend fun updatePregnancyCheck(eventId: String, date: LocalDate, result: PregnancyCheckResult)
}
