package com.herdmanager.app.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herdmanager.app.domain.model.Animal
import com.herdmanager.app.domain.model.FarmSettings
import com.herdmanager.app.domain.repository.AnimalRepository
import com.herdmanager.app.domain.repository.HerdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class EditAnimalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val animalRepository: AnimalRepository,
    private val herdRepository: HerdRepository
) : ViewModel() {

    private val animalId: String = checkNotNull(savedStateHandle["animalId"])

    private val _animal = MutableStateFlow<Animal?>(null)
    val animal: kotlinx.coroutines.flow.StateFlow<Animal?> = _animal

    val animalLoaded = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            _animal.value = withContext(Dispatchers.IO) { animalRepository.getAnimalById(animalId) }
            animalLoaded.value = true
        }
    }

    val herds = herdRepository.observeHerdsByFarm(FarmSettings.DEFAULT_FARM_ID)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val allAnimals = animalRepository.observeAnimalsByFarm(FarmSettings.DEFAULT_FARM_ID)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val selectedHerdId = MutableStateFlow<String?>(null)
    val selectedSireId = MutableStateFlow<String?>(null)
    val selectedDamId = MutableStateFlow<String?>(null)

    fun setSelectedHerd(herdId: String?) {
        selectedHerdId.value = herdId
    }

    fun setSelectedSire(sireId: String?) {
        selectedSireId.value = sireId
    }

    fun setSelectedDam(damId: String?) {
        selectedDamId.value = damId
    }

    private val _updateResult = MutableSharedFlow<UpdateAnimalResult>(replay = 0)
    val updateResult: SharedFlow<UpdateAnimalResult> = _updateResult

    fun updateAnimal(animal: Animal, newHerdId: String?) {
        viewModelScope.launch {
            if (animal.dateOfBirth > LocalDate.now()) {
                _updateResult.emit(UpdateAnimalResult.Error("Date of birth cannot be in the future."))
                return@launch
            }
            val animalToSave = animal.copy(
                sireId = selectedSireId.value.takeIf { !it.isNullOrBlank() },
                damId = selectedDamId.value.takeIf { !it.isNullOrBlank() }
            )
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    animalRepository.insertAnimal(animalToSave)
                    if (newHerdId != null && newHerdId != animal.currentHerdId) {
                        herdRepository.assignAnimalToHerd(animalId, newHerdId, LocalDate.now(), null)
                    }
                    UpdateAnimalResult.Success
                }.getOrElse {
                    when {
                        it is IllegalArgumentException && it.message?.contains("Ear tag") == true -> UpdateAnimalResult.Error(it.message!!)
                        it is android.database.sqlite.SQLiteConstraintException -> UpdateAnimalResult.Error("Ear tag already exists")
                        else -> UpdateAnimalResult.Error("Could not save: ${it.message}")
                    }
                }
            }
            _updateResult.emit(result)
        }
    }
}

sealed class UpdateAnimalResult {
    data object Success : UpdateAnimalResult()
    data class Error(val message: String) : UpdateAnimalResult()
}
