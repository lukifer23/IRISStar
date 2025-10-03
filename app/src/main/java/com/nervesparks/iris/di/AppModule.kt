package com.nervesparks.iris.di

import android.content.Context
import android.llama.cpp.LLamaAndroid
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.llm.ModelLoader
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
    fun provideModelLoader(llamaAndroid: LLamaAndroid): ModelLoader {
        return ModelLoader(llamaAndroid)
    }
}