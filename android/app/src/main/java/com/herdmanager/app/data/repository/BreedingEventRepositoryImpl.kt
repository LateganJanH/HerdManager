package com.herdmanager.app.data.repository

import com.herdmanager.app.data.local.dao.BreedingEventDao
import com.herdmanager.app.data.local.entity.BreedingEventEntity
import com.herdmanager.app.domain.model.BreedingEvent
import com.herdmanager.app.domain.model.BreedingEventType
import com.herdmanager.app.domain.model.PregnancyCheckResult
import com.herdmanager.app.domain.repository.BreedingEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class BreedingEventRepositoryImpl(
    private val dao: BreedingEventDao
) : BreedingEventRepository {

    override fun observeBreedingEventsByAnimal(animalId: String): Flow<List<BreedingEvent>> =
        dao.observeByAnimal(animalId).map { it.map { e -> e.toDomain() } }

    override fun observeAllBreedingEvents(): Flow<List<BreedingEvent>> =
        dao.observeAll().map { it.map { e -> e.toDomain() } }

    override suspend fun getBreedingEventById(id: String): BreedingEvent? =
        dao.getById(id)?.toDomain()

    override suspend fun insertBreedingEvent(event: BreedingEvent) {
        dao.insert(event.toEntity())
    }

    override suspend fun updatePregnancyCheck(eventId: String, date: LocalDate, result: PregnancyCheckResult) {
        val existing = dao.getById(eventId) ?: return
        dao.insert(
            existing.copy(
                pregnancyCheckDateEpochDay = date.toEpochDay(),
                pregnancyCheckResult = result.name
            )
        )
    }
}

private fun BreedingEventEntity.toDomain() = BreedingEvent(
    id = id,
    animalId = animalId,
    sireIds = sireIds,
    eventType = BreedingEventType.valueOf(eventType),
    serviceDate = LocalDate.ofEpochDay(serviceDate),
    notes = notes,
    pregnancyCheckDate = pregnancyCheckDateEpochDay?.let { LocalDate.ofEpochDay(it) },
    pregnancyCheckResult = pregnancyCheckResult?.let { PregnancyCheckResult.valueOf(it) }
)

private fun BreedingEvent.toEntity() = BreedingEventEntity(
    id = id,
    animalId = animalId,
    sireIds = sireIds,
    eventType = eventType.name,
    serviceDate = serviceDate.toEpochDay(),
    notes = notes,
    createdAt = System.currentTimeMillis(),
    pregnancyCheckDateEpochDay = pregnancyCheckDate?.toEpochDay(),
    pregnancyCheckResult = pregnancyCheckResult?.name
)
