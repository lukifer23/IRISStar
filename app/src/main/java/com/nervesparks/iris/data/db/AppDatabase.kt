package com.nervesparks.iris.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Chat::class, Message::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}