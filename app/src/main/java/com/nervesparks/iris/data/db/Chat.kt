package com.nervesparks.iris.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val created: Long = System.currentTimeMillis(),
    val updated: Long = System.currentTimeMillis()
)