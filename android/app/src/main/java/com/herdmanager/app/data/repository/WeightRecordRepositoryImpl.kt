package com.herdmanager.app.data.repository

import com.herdmanager.app.data.local.dao.WeightRecordDao
import com.herdmanager.app.data.local.entity.WeightRecordEntity
import com.herdmanager.app.domain.model.WeightRecord
import com.herdmanager.app.domain.repository.WeightRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class WeightRecordRepositoryImpl(
    private val dao: WeightRecordDao
) : WeightRecordRepository {

    override fun observeWeightRecordsByAnimal(animalId: String): Flow<List<WeightRecord>> =
        dao.observeByAnimal(animalId).map { it.map { e -> e.toDomain() } }

    override fun observeAllWeightRecords(): Flow<List<WeightRecord>> =
        dao.observeAll().map { it.map { e -> e.toDomain() } }

    override suspend fun insertWeightRecord(record: WeightRecord) {
        dao.insert(record.toEntity())
    }

    override suspend fun updateWeightRecord(record: WeightRecord) {
        dao.insert(record.toEntity())
    }

    override suspend fun deleteWeightRecord(id: String) {
        dao.deleteById(id)
    }
}

private fun WeightRecordEntity.toDomain() = WeightRecord(
    id = id,
    animalId = animalId,
    date = LocalDate.ofEpochDay(date),
    weightKg = weightKg,
    note = note
)

private fun WeightRecord.toEntity() = WeightRecordEntity(
    id = id,
    animalId = animalId,
    date = date.toEpochDay(),
    weightKg = weightKg,
    note = note
)
