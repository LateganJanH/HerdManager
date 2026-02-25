package com.herdmanager.app.domain.model

import java.time.Instant

data class Photo(
    val id: String,
    val animalId: String,
    val angle: PhotoAngle,
    val uri: String,
    val capturedAt: Instant,
    val latitude: Double? = null,
    val longitude: Double? = null
)

enum class PhotoAngle { LEFT_SIDE, RIGHT_SIDE, FACE, REAR }
