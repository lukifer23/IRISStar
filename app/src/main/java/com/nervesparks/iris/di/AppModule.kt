package com.nervesparks.iris.di

import android.content.Context
import android.llama.cpp.LLamaAndroid
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.data.exceptions.ErrorHandler
import com.nervesparks.iris.llm.EmbeddingService
import com.nervesparks.iris.llm.ModelLoader
import com.nervesparks.iris.llm.ModelPerformanceTracker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLlamaAndroid(): LLamaAndroid {
        return LLamaAndroid.instance()
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(@ApplicationContext context: Context): UserPreferencesRepository {
        return UserPreferencesRepository.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideModelLoader(
        llamaAndroid: LLamaAndroid,
        modelPerformanceTracker: ModelPerformanceTracker
    ): ModelLoader {
        return ModelLoader(llamaAndroid, modelPerformanceTracker)
    }

    @Provides
    @Singleton
    fun provideModelPerformanceTracker(): ModelPerformanceTracker {
        return ModelPerformanceTracker()
    }

    @Provides
    @Singleton
    @Named("embedding_dispatcher")
    fun provideEmbeddingDispatcher(): CoroutineDispatcher {
        return Dispatchers.Default
    }

    @Provides
    @Singleton
    fun provideEmbeddingService(
        llamaAndroid: LLamaAndroid,
        @Named("embedding_dispatcher") dispatcher: CoroutineDispatcher
    ): EmbeddingService {
        return EmbeddingService(llamaAndroid, dispatcher)
    }

    @Provides
    @Singleton
    fun provideErrorHandler(): ErrorHandler {
        return ErrorHandler
    }
}
