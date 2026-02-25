package com.herdmanager.app.domain.repository

import com.herdmanager.app.domain.model.Herd
import com.herdmanager.app.domain.model.HerdAssignment
import kotlinx.coroutines.flow.Flow

interface HerdRepository {
    fun observeHerdsByFarm(farmId: String): Flow<List<Herd>>
    suspend fun getHerdById(id: String): Herd?
    suspend fun insertHerd(herd: Herd)
    suspend fun deleteHerd(id: String)

    fun observeAssignmentsByAnimal(animalId: String): Flow<List<HerdAssignment>>
    suspend fun getAssignmentsByAnimal(animalId: String): List<HerdAssignment>
    suspend fun getCurrentAssignment(animalId: String): HerdAssignment?
    suspend fun assignAnimalToHerd(animalId: String, herdId: String, date: java.time.LocalDate, reason: String? = null)
}
