package com.nervesparks.iris.di

import com.nervesparks.iris.data.repository.ChatRepository
import com.nervesparks.iris.data.repository.ModelRepository
import com.nervesparks.iris.data.repository.SettingsRepository
import com.nervesparks.iris.data.repository.impl.ChatRepositoryImpl
import com.nervesparks.iris.data.repository.impl.ModelRepositoryImpl
import com.nervesparks.iris.data.repository.impl.SettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(chatRepositoryImpl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindModelRepository(modelRepositoryImpl: ModelRepositoryImpl): ModelRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(settingsRepositoryImpl: SettingsRepositoryImpl): SettingsRepository
}
