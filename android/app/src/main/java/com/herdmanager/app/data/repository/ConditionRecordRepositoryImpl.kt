package com.herdmanager.app.data.repository

import com.herdmanager.app.data.local.dao.ConditionRecordDao
import com.herdmanager.app.data.local.entity.ConditionRecordEntity
import com.herdmanager.app.domain.model.ConditionRecord
import com.herdmanager.app.domain.repository.ConditionRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class ConditionRecordRepositoryImpl(
    private val dao: ConditionRecordDao
) : ConditionRecordRepository {

    override fun observeByAnimal(animalId: String): Flow<List<ConditionRecord>> =
        dao.observeByAnimal(animalId).map { list -> list.map { it.toDomain() } }

    override fun observeAll(): Flow<List<ConditionRecord>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun insert(record: ConditionRecord) {
        dao.insert(record.toEntity())
    }

    override suspend fun update(record: ConditionRecord) {
        dao.insert(record.toEntity())
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }
}

private fun ConditionRecordEntity.toDomain(): ConditionRecord =
    ConditionRecord(
        id = id,
        animalId = animalId,
        date = LocalDate.ofEpochDay(dateEpochDay),
        score = score,
        notes = notes
    )

private fun ConditionRecord.toEntity(): ConditionRecordEntity =
    ConditionRecordEntity(
        id = id,
        animalId = animalId,
        dateEpochDay = date.toEpochDay(),
        score = score,
        notes = notes,
        updatedAt = System.currentTimeMillis()
    )

