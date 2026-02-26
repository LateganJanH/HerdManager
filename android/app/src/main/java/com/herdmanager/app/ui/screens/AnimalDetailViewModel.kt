package com.herdmanager.app.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herdmanager.app.domain.model.Animal
import com.herdmanager.app.domain.model.AnimalStatus
import com.herdmanager.app.domain.model.BreedingEvent
import com.herdmanager.app.domain.model.BreedingEventType
import com.herdmanager.app.domain.model.PregnancyCheckResult
import com.herdmanager.app.domain.model.CalvingEvent
import com.herdmanager.app.domain.model.FarmSettings
import com.herdmanager.app.domain.model.HealthEvent
import com.herdmanager.app.domain.model.HealthEventType
import com.herdmanager.app.domain.model.Sex
import com.herdmanager.app.domain.repository.AnimalRepository
import com.herdmanager.app.domain.repository.BreedingEventRepository
import com.herdmanager.app.domain.repository.FarmSettingsRepository
import com.herdmanager.app.domain.repository.HerdRepository
import com.herdmanager.app.domain.model.Photo
import com.herdmanager.app.domain.model.PhotoAngle
import com.herdmanager.app.domain.repository.CalvingEventRepository
import com.herdmanager.app.domain.model.WeightRecord
import com.herdmanager.app.domain.repository.HealthEventRepository
import com.herdmanager.app.domain.repository.PhotoRepository
import com.herdmanager.app.domain.repository.WeightRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

enum class AnimalDetailOperation { BREEDING, CALVING, PREGNANCY_CHECK, HEALTH, WEIGHT, PHOTO, AVATAR, DELETE_PHOTO, DELETE_HEALTH, DELETE_WEIGHT, DELETE_ANIMAL, TRANSFER }

sealed class AnimalDetailOperationResult {
    data class Success(val operation: AnimalDetailOperation, val message: String) : AnimalDetailOperationResult()
    data class Error(val operation: AnimalDetailOperation, val message: String) : AnimalDetailOperationResult()
}

data class BreedingEventWithCalving(
    val event: BreedingEvent,
    val calvings: List<CalvingEvent>
)

@HiltViewModel
class AnimalDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val animalRepository: AnimalRepository,
    private val herdRepository: HerdRepository,
    private val breedingEventRepository: BreedingEventRepository,
    private val calvingEventRepository: CalvingEventRepository,
    private val photoRepository: PhotoRepository,
    private val healthEventRepository: HealthEventRepository,
    private val weightRecordRepository: WeightRecordRepository,
    private val farmSettingsRepository: FarmSettingsRepository
) : ViewModel() {

    private val animalId: String = checkNotNull(savedStateHandle["animalId"])

    private val _operationResult = MutableSharedFlow<AnimalDetailOperationResult>(replay = 0)
    val operationResult: SharedFlow<AnimalDetailOperationResult> = _operationResult

    val animal = animalRepository.observeAnimalsByFarm(FarmSettings.DEFAULT_FARM_ID)
        .map { list -> list.find { it.id == animalId } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val breedingEvents = breedingEventRepository.observeBreedingEventsByAnimal(animalId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val photos = photoRepository.observePhotosByAnimal(animalId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val healthEvents = healthEventRepository.observeHealthEventsByAnimal(animalId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val weightRecords = weightRecordRepository.observeWeightRecordsByAnimal(animalId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val herds = herdRepository.observeHerdsByFarm(FarmSettings.DEFAULT_FARM_ID)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val sires = animalRepository.observeAnimalsByFarm(FarmSettings.DEFAULT_FARM_ID)
        .map { list -> list.filter { it.sex == Sex.MALE && it.id != animalId } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Sire and dam (pedigree parents) for display on detail screen. */
    val sireAndDam = combine(
        animal,
        animalRepository.observeAnimalsByFarm(FarmSettings.DEFAULT_FARM_ID)
    ) { a, all ->
        val sire = a?.sireId?.let { sid -> all.find { it.id == sid } }
        val dam = a?.damId?.let { did -> all.find { it.id == did } }
        Pair(sire, dam)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Pair(null as Animal?, null as Animal?)
    )

    val gestationDays = farmSettingsRepository.farmSettings()
        .map { it.gestationDaysClamped() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FarmSettings.DEFAULT_GESTATION_DAYS
        )

    val breedingEventsWithCalving = combine(
        breedingEventRepository.observeBreedingEventsByAnimal(animalId),
        calvingEventRepository.observeCalvingEventsByDam(animalId)
    ) { events, allCalvings ->
        events.map { event ->
            BreedingEventWithCalving(
                event = event,
                calvings = allCalvings.filter { it.breedingEventId == event.id }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun recordBreeding(
        serviceDate: LocalDate,
        eventType: BreedingEventType = BreedingEventType.NATURAL,
        sireIds: List<String> = emptyList(),
        notes: String? = null
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val animal = animalRepository.getAnimalById(animalId)
                if (animal?.sex != Sex.FEMALE) {
                    AnimalDetailOperationResult.Error(AnimalDetailOperation.BREEDING, "Only female animals can have breeding service recorded.")
                } else {
                    runCatching {
                        breedingEventRepository.insertBreedingEvent(
                            BreedingEvent(
                                id = UUID.randomUUID().toString(),
                                animalId = animalId,
                                sireIds = sireIds,
                                eventType = eventType,
                                serviceDate = serviceDate,
                                notes = notes?.takeIf { it.isNotBlank() }
                            )
                        )
                        AnimalDetailOperationResult.Success(AnimalDetailOperation.BREEDING, "Service recorded")
                    }.getOrElse {
                        AnimalDetailOperationResult.Error(AnimalDetailOperation.BREEDING, it.message ?: "Could not record breeding")
                    }
                }
            }
            _operationResult.emit(result)
        }
    }

    fun recordCalving(
        breedingEventId: String,
        actualDate: LocalDate,
        assistanceRequired: Boolean,
        calfSex: Sex?,
        calfWeight: Double?,
        createCalf: Boolean,
        calfEarTag: String,
        notes: String? = null
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val animal = animalRepository.getAnimalById(animalId)
                if (animal?.sex != Sex.FEMALE) {
                    AnimalDetailOperationResult.Error(AnimalDetailOperation.CALVING, "Only female animals (dams) can have calving recorded.")
                } else runCatching {
                    val calfId = if (createCalf) {
                        val calf = Animal(
                            id = UUID.randomUUID().toString(),
                            earTagNumber = calfEarTag,
                            sex = calfSex ?: Sex.MALE,
                            breed = "Unknown",
                            dateOfBirth = actualDate,
                            farmId = FarmSettings.DEFAULT_FARM_ID,
                            avatarPhotoId = null,
                            isCastrated = false,
                            status = AnimalStatus.ACTIVE,
                            damId = animalId
                        )
                        animalRepository.insertAnimal(calf)
                        calf.id
                    } else null
                    calvingEventRepository.insertCalvingEvent(
                        CalvingEvent(
                            id = UUID.randomUUID().toString(),
                            damId = animalId,
                            calfId = calfId,
                            breedingEventId = breedingEventId,
                            actualDate = actualDate,
                            assistanceRequired = assistanceRequired,
                            calfSex = calfSex,
                            calfWeight = calfWeight,
                            notes = notes
                        )
                    )
                    AnimalDetailOperationResult.Success(AnimalDetailOperation.CALVING, "Calving recorded")
                }.getOrElse { e ->
                    val msg = when {
                        e is IllegalArgumentException && e.message?.contains("Ear tag") == true -> e.message!!
                        e is android.database.sqlite.SQLiteConstraintException -> "Calf ear tag already exists"
                        else -> "Could not record calving: ${e.message}"
                    }
                    AnimalDetailOperationResult.Error(AnimalDetailOperation.CALVING, msg)
                }
            }
            _operationResult.emit(result)
        }
    }

    fun recordPregnancyCheck(
        breedingEventId: String,
        checkDate: LocalDate,
        result: PregnancyCheckResult
    ) {
        viewModelScope.launch {
            val opResult = withContext(Dispatchers.IO) {
                val event = breedingEventRepository.getBreedingEventById(breedingEventId)
                if (event == null) {
                    AnimalDetailOperationResult.Error(AnimalDetailOperation.PREGNANCY_CHECK, "Breeding event not found")
                } else {
                    runCatching {
                        breedingEventRepository.updatePregnancyCheck(breedingEventId, checkDate, result)
                        AnimalDetailOperationResult.Success(AnimalDetailOperation.PREGNANCY_CHECK, "Pregnancy check recorded")
                    }.getOrElse {
                        AnimalDetailOperationResult.Error(AnimalDetailOperation.PREGNANCY_CHECK, it.message ?: "Could not record pregnancy check")
                    }
                }
            }
            _operationResult.emit(opResult)
        }
    }

    fun addPhoto(uri: String, angle: PhotoAngle, latitude: Double? = null, longitude: Double? = null) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    photoRepository.insertPhoto(
                        Photo(
                            id = UUID.randomUUID().toString(),
                            animalId = animalId,
                            angle = angle,
                            uri = uri,
                            capturedAt = java.time.Instant.now(),
                            latitude = latitude,
                            longitude = longitude
                        )
                    )
                    AnimalDetailOperationResult.Success(AnimalDetailOperation.PHOTO, "Photo added")
                }.getOrElse {
                    AnimalDetailOperationResult.Error(AnimalDetailOperation.PHOTO, it.message ?: "Could not add photo")
                }
            }
            _operationResult.emit(result)
        }
    }

    fun addHealthEvent(
        eventType: HealthEventType,
        date: LocalDate,
        product: String?,
        dosage: String?,
        withdrawalPeriodEnd: LocalDate?,
        notes: String?
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    healthEventRepository.insertHealthEvent(
                        HealthEvent(
                            id = UUID.randomUUID().toString(),
                            animalId = animalId,
                            eventType = eventType,
                            date = date,
                            product = product?.takeIf { it.isNotBlank() },
                            dosage = dosage?.takeIf { it.isNotBlank() },
                            withdrawalPeriodEnd = withdrawalPeriodEnd,
                            notes = notes?.takeIf { it.isNotBlank() }
                        )
                    )
                    AnimalDetailOperationResult.Success(AnimalDetailOperation.HEALTH, "Health event logged")
                }.getOrElse {
                    AnimalDetailOperationResult.Error(AnimalDetailOperation.HEALTH, it.message ?: "Could not add health event")
                }
            }
            _operationResult.emit(result)
        }
    }

    fun addWeightRecord(date: LocalDate, weightKg: Double, note: String?) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    weightRecordRepository.insertWeightRecord(
                        WeightRecord(
                            id = UUID.randomUUID().toString(),
                            animalId = animalId,
                            date = date,
                            weightKg = weightKg,
                            note = note?.takeIf { it.isNotBlank() }
                        )
                    )
                    AnimalDetailOperationResult.Success(AnimalDetailOperation.WEIGHT, "Weight recorded")
                }.getOrElse {
                    AnimalDetailOperationResult.Error(AnimalDetailOperation.WEIGHT, it.message ?: "Could not add weight")
                }
            }
            _operationResult.emit(result)
        }
    }

    fun setAvatarPhoto(photoId: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val current = animalRepository.getAnimalById(animalId)
                if (current == null) {
                    AnimalDetailOperationResult.Error(AnimalDetailOperation.AVATAR, "Animal not found")
                } else {
                    runCatching {
                        animalRepository.insertAnimal(current.copy(avatarPhotoId = photoId))
                        AnimalDetailOperationResult.Success(AnimalDetailOperation.AVATAR, "Avatar updated")
                    }.getOrElse {
                        AnimalDetailOperationResult.Error(AnimalDetailOperation.AVATAR, it.message ?: "Could not set avatar")
                    }
                }
            }
            _operationResult.emit(result)
        }
    }

    fun deletePhoto(photo: Photo) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val current = animalRepository.getAnimalById(animalId)
                    photoRepository.deletePhoto(photo)
                    if (current?.avatarPhotoId == photo.id) {
                        current.let { animalRepository.insertAnimal(it.copy(avatarPhotoId = null)) }
                    }
                    AnimalDetailOperationResult.Success(AnimalDetailOperation.DELETE_PHOTO, "Photo removed")
                }.getOrElse {
                    AnimalDetailOperationResult.Error(AnimalDetailOperation.DELETE_PHOTO, it.message ?: "Could not delete photo")
                }
            }
            _operationResult.emit(result)
        }
    }

    fun deleteHealthEvent(id: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    healthEventRepository.deleteHealthEvent(id)
                    AnimalDetailOperationResult.Success(AnimalDetailOperation.DELETE_HEALTH, "Health event removed")
                }.getOrElse {
                    AnimalDetailOperationResult.Error(AnimalDetailOperation.DELETE_HEALTH, it.message ?: "Could not delete")
                }
            }
            _operationResult.emit(result)
        }
    }

    fun deleteWeightRecord(id: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    weightRecordRepository.deleteWeightRecord(id)
                    AnimalDetailOperationResult.Success(AnimalDetailOperation.DELETE_WEIGHT, "Weight record removed")
                }.getOrElse {
                    AnimalDetailOperationResult.Error(AnimalDetailOperation.DELETE_WEIGHT, it.message ?: "Could not delete")
                }
            }
            _operationResult.emit(result)
        }
    }

    fun transferToHerd(herdId: String, date: java.time.LocalDate, reason: String?) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    herdRepository.assignAnimalToHerd(animalId, herdId, date, reason)
                    AnimalDetailOperationResult.Success(AnimalDetailOperation.TRANSFER, "Animal transferred")
                }.getOrElse {
                    AnimalDetailOperationResult.Error(AnimalDetailOperation.TRANSFER, it.message ?: "Could not transfer")
                }
            }
            _operationResult.emit(result)
        }
    }

    fun deleteAnimal() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    animalRepository.deleteAnimal(animalId)
                    AnimalDetailOperationResult.Success(AnimalDetailOperation.DELETE_ANIMAL, "Animal deleted")
                }.getOrElse {
                    AnimalDetailOperationResult.Error(AnimalDetailOperation.DELETE_ANIMAL, it.message ?: "Could not delete animal")
                }
            }
            _operationResult.emit(result)
        }
    }
}
