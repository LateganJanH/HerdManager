package com.herdmanager.app.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.herdmanager.app.data.local.AppDatabase
import com.herdmanager.app.domain.repository.AuthRepository
import com.herdmanager.app.data.repository.AnimalRepositoryImpl
import com.herdmanager.app.data.repository.BreedingEventRepositoryImpl
import com.herdmanager.app.data.repository.HerdRepositoryImpl
import com.herdmanager.app.data.repository.CalvingEventRepositoryImpl
import com.herdmanager.app.data.repository.BackupRepositoryImpl
import com.herdmanager.app.data.repository.HealthEventRepositoryImpl
import com.herdmanager.app.data.repository.PhotoRepositoryImpl
import com.herdmanager.app.data.repository.WeightRecordRepositoryImpl
import com.herdmanager.app.domain.repository.AnimalRepository
import com.herdmanager.app.domain.repository.BackupRepository
import com.herdmanager.app.domain.repository.HerdRepository
import com.herdmanager.app.domain.repository.BreedingEventRepository
import com.herdmanager.app.domain.repository.CalvingEventRepository
import com.herdmanager.app.domain.repository.FarmSettingsRepository
import com.herdmanager.app.domain.repository.HealthEventRepository
import com.herdmanager.app.domain.repository.PhotoRepository
import com.herdmanager.app.domain.repository.WeightRecordRepository
import com.herdmanager.app.data.repository.FarmSettingsRepositoryImpl
import com.herdmanager.app.data.repository.SyncRepositoryImpl
import com.herdmanager.app.data.repository.ThemePreferencesRepositoryImpl
import com.herdmanager.app.domain.repository.SyncRepository
import com.herdmanager.app.domain.repository.ThemePreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "herdmanager.db"
    ).fallbackToDestructiveMigration(dropAllTables = true).build()

    @Provides
    @Singleton
    fun provideAnimalRepository(db: AppDatabase): AnimalRepository =
        AnimalRepositoryImpl(db.animalDao())

    @Provides
    @Singleton
    fun provideHerdRepository(db: AppDatabase): HerdRepository =
        HerdRepositoryImpl(db.herdDao(), db.herdAssignmentDao(), db.animalDao())

    @Provides
    @Singleton
    fun provideBreedingEventRepository(db: AppDatabase): BreedingEventRepository =
        BreedingEventRepositoryImpl(db.breedingEventDao())

    @Provides
    @Singleton
    fun provideCalvingEventRepository(db: AppDatabase): CalvingEventRepository =
        CalvingEventRepositoryImpl(db.calvingEventDao())

    @Provides
    @Singleton
    fun providePhotoRepository(db: AppDatabase): PhotoRepository =
        PhotoRepositoryImpl(db.photoDao())

    @Provides
    @Singleton
    fun provideHealthEventRepository(db: AppDatabase): HealthEventRepository =
        HealthEventRepositoryImpl(db.healthEventDao())

    @Provides
    @Singleton
    fun provideWeightRecordRepository(db: AppDatabase): WeightRecordRepository =
        WeightRecordRepositoryImpl(db.weightRecordDao())

    @Provides
    @Singleton
    fun provideFarmSettingsRepository(
        @ApplicationContext context: Context
    ): FarmSettingsRepository =
        FarmSettingsRepositoryImpl(context)

    @Provides
    @Singleton
    fun provideThemePreferencesRepository(
        @ApplicationContext context: Context
    ): ThemePreferencesRepository =
        ThemePreferencesRepositoryImpl(context)

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideSyncRepository(
        @ApplicationContext context: Context,
        firestore: FirebaseFirestore,
        storage: FirebaseStorage,
        authRepository: AuthRepository,
        farmSettingsRepository: FarmSettingsRepository,
        db: AppDatabase
    ): SyncRepository =
        SyncRepositoryImpl(
            context,
            firestore,
            storage,
            authRepository,
            db.animalDao(),
            db.herdDao(),
            db.herdAssignmentDao(),
            db.breedingEventDao(),
            db.calvingEventDao(),
            db.healthEventDao(),
            db.weightRecordDao(),
            db.photoDao(),
            farmSettingsRepository
        )

    @Provides
    @Singleton
    fun provideBackupRepository(
        db: AppDatabase,
        farmSettingsRepository: FarmSettingsRepository
    ): BackupRepository =
        BackupRepositoryImpl(
            db.animalDao(),
            db.herdDao(),
            db.herdAssignmentDao(),
            db.breedingEventDao(),
            db.calvingEventDao(),
            db.healthEventDao(),
            db.weightRecordDao(),
            db.photoDao(),
            farmSettingsRepository
        )
}
