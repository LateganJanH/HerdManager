package com.herdmanager.app.di

import com.google.firebase.firestore.FirebaseFirestore
import com.herdmanager.app.data.repository.AppConfigRepositoryImpl
import com.herdmanager.app.domain.repository.AppConfigRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {

    @Provides
    @Singleton
    fun provideAppConfigRepository(firestore: FirebaseFirestore): AppConfigRepository =
        AppConfigRepositoryImpl(firestore)
}
