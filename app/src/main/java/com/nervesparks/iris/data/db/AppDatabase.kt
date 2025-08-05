package com.nervesparks.iris.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Chat::class, Message::class, Memory::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}