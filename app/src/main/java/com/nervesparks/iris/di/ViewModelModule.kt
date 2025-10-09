package com.nervesparks.iris.di

import com.nervesparks.iris.viewmodel.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * PHASE 1.7: ViewModelModule - Provides specialized ViewModels
 * This module provides the new specialized ViewModels that replace
 * the monolithic MainViewModel architecture.
 */
@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {

    @Provides
    @ViewModelScoped
    fun provideChatViewModel(
        chatRepository: com.nervesparks.iris.data.repository.ChatRepository,
        documentRepository: com.nervesparks.iris.data.DocumentRepository
    ): ChatViewModel {
        return ChatViewModel(chatRepository, documentRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideModelViewModel(
        llamaAndroid: android.llama.cpp.LLamaAndroid,
        modelLoader: com.nervesparks.iris.llm.ModelLoader,
        modelPerformanceTracker: com.nervesparks.iris.llm.ModelPerformanceTracker,
        userPreferencesRepository: com.nervesparks.iris.data.UserPreferencesRepository,
        modelRepository: com.nervesparks.iris.data.repository.ModelRepository
    ): ModelViewModel {
        return ModelViewModel(
            llamaAndroid,
            modelLoader,
            modelPerformanceTracker,
            userPreferencesRepository,
            modelRepository
        )
    }

    @Provides
    @ViewModelScoped
    fun provideSearchViewModel(
        webSearcher: com.nervesparks.iris.data.WebSearcher,
        androidSearcher: com.nervesparks.iris.data.AndroidSearcher,
        documentRepository: com.nervesparks.iris.data.DocumentRepository,
        embeddingService: com.nervesparks.iris.llm.EmbeddingService
    ): SearchViewModel {
        return SearchViewModel(
            webSearcher,
            androidSearcher,
            documentRepository,
            embeddingService
        )
    }

    @Provides
    @ViewModelScoped
    fun provideVoiceViewModel(): VoiceViewModel {
        return VoiceViewModel()
    }

    @Provides
    @ViewModelScoped
    fun provideSettingsViewModel(
        userPreferencesRepository: com.nervesparks.iris.data.UserPreferencesRepository,
        settingsRepository: com.nervesparks.iris.data.repository.SettingsRepository
    ): SettingsViewModel {
        return SettingsViewModel(userPreferencesRepository, settingsRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideBenchmarkViewModel(
        llamaAndroid: android.llama.cpp.LLamaAndroid,
        userPreferencesRepository: com.nervesparks.iris.data.UserPreferencesRepository
    ): BenchmarkViewModel {
        return BenchmarkViewModel(llamaAndroid, userPreferencesRepository)
    }
}
