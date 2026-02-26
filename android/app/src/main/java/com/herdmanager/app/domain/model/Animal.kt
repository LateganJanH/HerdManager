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
