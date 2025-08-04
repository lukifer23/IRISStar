package com.nervesparks.iris.di

import android.content.Context
import androidx.room.Room
import com.nervesparks.iris.data.ChatRepository
import com.nervesparks.iris.data.db.AppDatabase
import com.nervesparks.iris.data.db.ChatDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "iris_chats.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideChatDao(appDatabase: AppDatabase): ChatDao {
        return appDatabase.chatDao()
    }
}
