package com.herdmanager.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herdmanager.app.domain.model.Animal
import com.herdmanager.app.domain.model.FarmSettings
import com.herdmanager.app.domain.repository.AnimalRepository
import com.herdmanager.app.domain.repository.HerdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AddAnimalViewModel @Inject constructor(
    private val animalRepository: AnimalRepository,
    private val herdRepository: HerdRepository
) : ViewModel() {

    private val _addResult = MutableSharedFlow<AddAnimalResult>(replay = 0)
    val addResult: SharedFlow<AddAnimalResult> = _addResult

    val herds = herdRepository.observeHerdsByFarm(FarmSettings.DEFAULT_FARM_ID)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addAnimal(animal: Animal, herdId: String? = null) {
        viewModelScope.launch {
            if (animal.dateOfBirth > LocalDate.now()) {
                _addResult.emit(AddAnimalResult.Error("Date of birth cannot be in the future."))
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    animalRepository.insertAnimal(animal)
                    herdId?.let { herdRepository.assignAnimalToHerd(animal.id, it, animal.dateOfBirth, null) }
                    AddAnimalResult.Success
                }.getOrElse {
                    when {
                        it is IllegalArgumentException && it.message?.contains("Ear tag") == true -> AddAnimalResult.Error(it.message!!)
                        it is android.database.sqlite.SQLiteConstraintException -> AddAnimalResult.Error("Ear tag already exists")
                        else -> AddAnimalResult.Error("Could not save: ${it.message}")
                    }
                }
            }
            _addResult.emit(result)
        }
    }
}

sealed class AddAnimalResult {
    data object Success : AddAnimalResult()
    data class Error(val message: String) : AddAnimalResult()
}
