package com.herdmanager.app.domain.model

import java.time.LocalDate
import java.time.ZoneOffset

data class Animal(
    val id: String,
    val earTagNumber: String,
    val rfid: String? = null,
    val name: String? = null,
    val sex: Sex,
    val breed: String,
    val dateOfBirth: LocalDate,
    val farmId: String,
    val currentHerdId: String? = null,
    val coatColor: String? = null,
    val hornStatus: HornStatus? = null,
    val isCastrated: Boolean? = null,
    val avatarPhotoId: String? = null,
    val status: AnimalStatus,
    /** Pedigree: parent animal IDs (Phase 2). */
    val sireId: String? = null,
    val damId: String? = null
)

enum class Sex { MALE, FEMALE }
enum class HornStatus { POLLED, HORNED, SCURED }
enum class AnimalStatus { ACTIVE, SOLD, DECEASED, CULLED, ACQUIRED_FROM }

/** True if the animal is still on farm (excludes SOLD, DECEASED, CULLED). Use for "current herd" counts, due-soon, and analytics. */
val AnimalStatus.isInCurrentHerd: Boolean
    get() = this != AnimalStatus.SOLD && this != AnimalStatus.DECEASED && this != AnimalStatus.CULLED

/** True if this animal is still on farm; use for current herd filters. */
val Animal.isInCurrentHerd: Boolean
    get() = status.isInCurrentHerd
