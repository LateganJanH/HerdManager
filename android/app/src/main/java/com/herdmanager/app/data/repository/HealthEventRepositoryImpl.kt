package com.herdmanager.app.data.repository

import com.herdmanager.app.data.local.dao.HealthEventDao
import com.herdmanager.app.data.local.entity.HealthEventEntity
import com.herdmanager.app.domain.model.HealthEvent
import com.herdmanager.app.domain.model.HealthEventType
import com.herdmanager.app.domain.repository.HealthEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class HealthEventRepositoryImpl(
    private val dao: HealthEventDao
) : HealthEventRepository {

    override fun observeHealthEventsByAnimal(animalId: String): Flow<List<HealthEvent>> =
        dao.observeByAnimal(animalId).map { it.map { e -> e.toDomain() } }

    override fun observeAllHealthEvents(): Flow<List<HealthEvent>> =
        dao.observeAll().map { it.map { e -> e.toDomain() } }

    override suspend fun insertHealthEvent(event: HealthEvent) {
        dao.insert(event.toEntity())
    }

    override suspend fun deleteHealthEvent(id: String) {
        dao.deleteById(id)
    }
}

private fun HealthEventEntity.toDomain() = HealthEvent(
    id = id,
    animalId = animalId,
    eventType = HealthEventType.valueOf(eventType),
    date = LocalDate.ofEpochDay(date),
    product = product,
    dosage = dosage,
    withdrawalPeriodEnd = withdrawalPeriodEnd?.let { LocalDate.ofEpochDay(it) },
    notes = notes
)

private fun HealthEvent.toEntity() = HealthEventEntity(
    id = id,
    animalId = animalId,
    eventType = eventType.name,
    date = date.toEpochDay(),
    product = product,
    dosage = dosage,
    withdrawalPeriodEnd = withdrawalPeriodEnd?.toEpochDay(),
    notes = notes
)
