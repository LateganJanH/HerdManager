package com.herdmanager.app.data.repository

import com.herdmanager.app.data.local.dao.AnimalDao
import com.herdmanager.app.data.local.entity.AnimalEntity
import com.herdmanager.app.domain.model.Animal
import com.herdmanager.app.domain.model.AnimalStatus
import com.herdmanager.app.domain.model.HornStatus
import com.herdmanager.app.domain.model.Sex
import com.herdmanager.app.domain.repository.AnimalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class AnimalRepositoryImpl(
    private val dao: AnimalDao
) : AnimalRepository {

    override fun observeAnimalsByFarm(farmId: String): Flow<List<Animal>> =
        dao.observeByFarm(farmId).map { entities -> entities.map { it.toDomain() } }

    override fun observeAnimalsByFarmAndHerd(farmId: String, herdId: String?): Flow<List<Animal>> =
        dao.observeByFarmAndHerd(farmId, herdId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getAnimalById(id: String): Animal? =
        dao.getById(id)?.toDomain()

    override suspend fun insertAnimal(animal: Animal) {
        if (animal.earTagNumber.isBlank()) {
            throw IllegalArgumentException("Ear tag is required")
        }
        if (animal.breed.isBlank()) {
            throw IllegalArgumentException("Breed is required")
        }
        val existing = dao.getByEarTagAndFarm(animal.earTagNumber, animal.farmId)
        if (existing != null && existing.id != animal.id) {
            throw IllegalArgumentException("Ear tag already exists")
        }
        dao.insert(animal.toEntity())
    }

    override suspend fun deleteAnimal(id: String) {
        dao.deleteById(id)
    }
}

private fun AnimalEntity.toDomain() = Animal(
    id = id,
    earTagNumber = earTagNumber,
    rfid = rfid,
    name = name,
    sex = Sex.valueOf(sex),
    breed = breed,
    dateOfBirth = LocalDate.ofEpochDay(dateOfBirth),
    farmId = farmId,
    currentHerdId = currentHerdId,
    coatColor = coatColor,
    hornStatus = hornStatus?.let { HornStatus.valueOf(it) },
    isCastrated = isCastrated,
    avatarPhotoId = avatarPhotoId,
    status = AnimalStatus.valueOf(status),
    sireId = sireId.takeIf { !it.isNullOrBlank() },
    damId = damId.takeIf { !it.isNullOrBlank() }
)

private fun Animal.toEntity() = AnimalEntity(
    id = id,
    earTagNumber = earTagNumber,
    rfid = rfid,
    name = name,
    sex = sex.name,
    breed = breed,
    dateOfBirth = dateOfBirth.toEpochDay(),
    farmId = farmId,
    currentHerdId = currentHerdId,
    coatColor = coatColor,
    hornStatus = hornStatus?.name,
    isCastrated = isCastrated,
    avatarPhotoId = avatarPhotoId,
    status = status.name,
    sireId = sireId.takeIf { !it.isNullOrBlank() },
    damId = damId.takeIf { !it.isNullOrBlank() },
    createdAt = System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis()
)
