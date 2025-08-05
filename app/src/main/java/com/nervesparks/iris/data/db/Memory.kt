package com.nervesparks.iris.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: Long,
    val summary: String,
    val created: Long = System.currentTimeMillis()
)
