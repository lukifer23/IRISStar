package com.nervesparks.iris.di

import android.content.Context
import com.nervesparks.iris.viewmodel.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import okhttp3.OkHttpClient

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
        documentRepository: com.nervesparks.iris.data.DocumentRepository,
        embeddingService: com.nervesparks.iris.llm.EmbeddingService
    ): ChatViewModel {
        return ChatViewModel(chatRepository, documentRepository, embeddingService)
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
        webSearchService: com.nervesparks.iris.data.WebSearchService,
        androidSearchService: com.nervesparks.iris.data.AndroidSearchService
    ): SearchViewModel {
        return SearchViewModel(webSearchService, androidSearchService)
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

    @Provides
    @ViewModelScoped
    fun provideDocumentViewModel(
        llamaAndroid: android.llama.cpp.LLamaAndroid,
        documentRepository: com.nervesparks.iris.data.DocumentRepository,
        embeddingService: com.nervesparks.iris.llm.EmbeddingService,
        errorHandler: com.nervesparks.iris.data.exceptions.ErrorHandler
    ): DocumentViewModel {
        return DocumentViewModel(llamaAndroid, documentRepository, embeddingService, errorHandler)
    }

    @Provides
    @ViewModelScoped
    fun provideGenerationViewModel(
        @dagger.hilt.android.qualifiers.ApplicationContext context: Context,
        errorHandler: com.nervesparks.iris.data.exceptions.ErrorHandler
    ): GenerationViewModel {
        return GenerationViewModel(context, errorHandler)
    }

    @Provides
    @ViewModelScoped
    fun provideToolViewModel(
        llamaAndroid: android.llama.cpp.LLamaAndroid,
        webSearchService: com.nervesparks.iris.data.WebSearchService,
        errorHandler: com.nervesparks.iris.data.exceptions.ErrorHandler
    ): ToolViewModel {
        return ToolViewModel(llamaAndroid, webSearchService, errorHandler)
    }

    @Provides
    @ViewModelScoped
    fun provideDownloadViewModel(
        @ApplicationContext appContext: Context,
        huggingFaceApiService: com.nervesparks.iris.data.HuggingFaceApiService,
        userPreferencesRepository: com.nervesparks.iris.data.UserPreferencesRepository,
        errorHandler: com.nervesparks.iris.data.exceptions.ErrorHandler,
        okHttpClient: OkHttpClient
    ): DownloadViewModel {
        return DownloadViewModel(appContext, huggingFaceApiService, userPreferencesRepository, errorHandler, okHttpClient)
    }
}
