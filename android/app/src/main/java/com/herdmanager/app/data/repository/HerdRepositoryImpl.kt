package com.herdmanager.app.data.repository

import com.herdmanager.app.data.local.dao.AnimalDao
import com.herdmanager.app.data.local.dao.HerdAssignmentDao
import com.herdmanager.app.data.local.dao.HerdDao
import com.herdmanager.app.data.local.entity.HerdAssignmentEntity
import com.herdmanager.app.data.local.entity.HerdEntity
import com.herdmanager.app.domain.model.Herd
import com.herdmanager.app.domain.model.HerdAssignment
import com.herdmanager.app.domain.repository.HerdRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

class HerdRepositoryImpl(
    private val herdDao: HerdDao,
    private val herdAssignmentDao: HerdAssignmentDao,
    private val animalDao: AnimalDao
) : HerdRepository {

    override fun observeHerdsByFarm(farmId: String): Flow<List<Herd>> =
        herdDao.observeByFarm(farmId).map { it.map { e -> e.toDomain() } }

    override suspend fun getHerdById(id: String): Herd? =
        herdDao.getById(id)?.toDomain()

    override suspend fun insertHerd(herd: Herd) {
        herdDao.insert(herd.toEntity())
    }

    override suspend fun deleteHerd(id: String) {
        animalDao.clearCurrentHerdForHerd(id, System.currentTimeMillis())
        herdAssignmentDao.deleteByHerd(id)
        herdDao.deleteById(id)
    }

    override fun observeAssignmentsByAnimal(animalId: String): Flow<List<HerdAssignment>> =
        herdAssignmentDao.observeByAnimal(animalId).map { it.map { e -> e.toDomain() } }

    override suspend fun getAssignmentsByAnimal(animalId: String): List<HerdAssignment> =
        herdAssignmentDao.getByAnimal(animalId).map { it.toDomain() }

    override suspend fun getCurrentAssignment(animalId: String): HerdAssignment? =
        herdAssignmentDao.getCurrentByAnimal(animalId)?.toDomain()

    override suspend fun assignAnimalToHerd(animalId: String, herdId: String, date: LocalDate, reason: String?) {
        val animal = animalDao.getById(animalId)
            ?: throw IllegalArgumentException("Animal not found")
        val dateEpochDay = date.toEpochDay()

        val current = herdAssignmentDao.getCurrentByAnimal(animalId)
        if (current?.herdId == herdId) return

        if (current != null) {
            herdAssignmentDao.insert(
                current.copy(removedAt = dateEpochDay, reason = reason)
            )
        }

        herdAssignmentDao.insert(
            HerdAssignmentEntity(
                id = UUID.randomUUID().toString(),
                animalId = animalId,
                herdId = herdId,
                assignedAt = dateEpochDay,
                removedAt = null,
                reason = null
            )
        )

        animalDao.insert(animal.copy(currentHerdId = herdId, updatedAt = System.currentTimeMillis()))
    }
}

private fun HerdEntity.toDomain() = Herd(
    id = id,
    name = name,
    farmId = farmId,
    description = description,
    sortOrder = sortOrder
)

private fun Herd.toEntity() = HerdEntity(
    id = id,
    name = name,
    farmId = farmId,
    description = description,
    sortOrder = sortOrder
)

private fun HerdAssignmentEntity.toDomain() = HerdAssignment(
    id = id,
    animalId = animalId,
    herdId = herdId,
    assignedAt = LocalDate.ofEpochDay(assignedAt),
    removedAt = removedAt?.let { LocalDate.ofEpochDay(it) },
    reason = reason
)
