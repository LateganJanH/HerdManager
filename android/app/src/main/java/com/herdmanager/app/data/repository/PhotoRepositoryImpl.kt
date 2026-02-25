package com.herdmanager.app.data.repository

import com.herdmanager.app.data.local.dao.PhotoDao
import com.herdmanager.app.data.local.entity.PhotoEntity
import com.herdmanager.app.domain.model.Photo
import com.herdmanager.app.domain.model.PhotoAngle
import com.herdmanager.app.domain.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class PhotoRepositoryImpl(
    private val dao: PhotoDao
) : PhotoRepository {

    override fun observePhotosByAnimal(animalId: String): Flow<List<Photo>> =
        dao.observeByAnimal(animalId).map { it.map { e -> e.toDomain() } }

    override fun observeAllPhotos(): Flow<List<Photo>> =
        dao.observeAll().map { it.map { e -> e.toDomain() } }

    override suspend fun insertPhoto(photo: Photo) {
        dao.insert(photo.toEntity())
    }

    override suspend fun deletePhoto(photo: Photo) {
        dao.delete(photo.toEntity())
    }
}

private fun PhotoEntity.toDomain() = Photo(
    id = id,
    animalId = animalId,
    angle = PhotoAngle.valueOf(angle),
    uri = uri,
    capturedAt = Instant.ofEpochMilli(capturedAt),
    latitude = latitude,
    longitude = longitude
)

private fun Photo.toEntity() = PhotoEntity(
    id = id,
    animalId = animalId,
    angle = angle.name,
    uri = uri,
    capturedAt = capturedAt.toEpochMilli(),
    latitude = latitude,
    longitude = longitude
)
