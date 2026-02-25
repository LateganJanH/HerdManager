package com.herdmanager.app.data.repository

import com.herdmanager.app.data.local.dao.CalvingEventDao
import com.herdmanager.app.data.local.entity.CalvingEventEntity
import com.herdmanager.app.domain.model.CalvingEvent
import com.herdmanager.app.domain.model.Sex
import com.herdmanager.app.domain.repository.CalvingEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class CalvingEventRepositoryImpl(
    private val dao: CalvingEventDao
) : CalvingEventRepository {

    override fun observeCalvedBreedingEventIds(): Flow<List<String>> =
        dao.observeCalvedBreedingEventIds().map { list -> list.map { it.id } }

    override fun observeCalvingEventsByDam(damId: String): Flow<List<CalvingEvent>> =
        dao.observeByDam(damId).map { it.map { e -> e.toDomain() } }

    override suspend fun getByBreedingEvent(breedingEventId: String): CalvingEvent? =
        dao.getByBreedingEvent(breedingEventId)?.toDomain()

    override suspend fun getAllCalvingEvents(): List<CalvingEvent> =
        dao.getAll().map { it.toDomain() }

    override suspend fun insertCalvingEvent(event: CalvingEvent) {
        dao.insert(event.toEntity())
    }
}

private fun CalvingEventEntity.toDomain() = CalvingEvent(
    id = id,
    damId = damId,
    calfId = calfId,
    breedingEventId = breedingEventId,
    actualDate = LocalDate.ofEpochDay(actualDate),
    assistanceRequired = assistanceRequired,
    calfSex = calfSex?.let { Sex.valueOf(it) },
    calfWeight = calfWeight,
    notes = notes
)

private fun CalvingEvent.toEntity() = CalvingEventEntity(
    id = id,
    damId = damId,
    calfId = calfId,
    breedingEventId = breedingEventId,
    actualDate = actualDate.toEpochDay(),
    assistanceRequired = assistanceRequired,
    calfSex = calfSex?.name,
    calfWeight = calfWeight,
    notes = notes
)
