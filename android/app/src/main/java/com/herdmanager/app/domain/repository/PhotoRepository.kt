package com.herdmanager.app.domain.repository

import com.herdmanager.app.domain.model.Photo
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {
    fun observePhotosByAnimal(animalId: String): Flow<List<Photo>>
    fun observeAllPhotos(): Flow<List<Photo>>
    suspend fun insertPhoto(photo: Photo)
    suspend fun deletePhoto(photo: Photo)
}
