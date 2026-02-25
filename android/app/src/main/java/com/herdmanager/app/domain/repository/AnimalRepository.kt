package com.herdmanager.app.domain.repository

import com.herdmanager.app.domain.model.Animal
import kotlinx.coroutines.flow.Flow

interface AnimalRepository {
    fun observeAnimalsByFarm(farmId: String): Flow<List<Animal>>
    fun observeAnimalsByFarmAndHerd(farmId: String, herdId: String?): Flow<List<Animal>>
    suspend fun getAnimalById(id: String): Animal?
    suspend fun insertAnimal(animal: Animal)
    suspend fun deleteAnimal(id: String)
}
