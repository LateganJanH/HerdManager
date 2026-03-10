package com.herdmanager.app

import com.herdmanager.app.di.AppConfigModule
import com.herdmanager.app.domain.repository.AppConfigRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AppConfigModule::class]
)
object TestAppConfigModule {

    @Provides
    @Singleton
    fun provideAppConfigRepository(): AppConfigRepository = FakeAppConfigRepository()
}
