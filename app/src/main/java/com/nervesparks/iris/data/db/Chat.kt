package com.nervesparks.iris.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val created: Long = System.currentTimeMillis(),
    val updated: Long = System.currentTimeMillis(),
    // Per-chat model settings
    val modelName: String? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val maxTokens: Int? = null,
    val contextLength: Int? = null,
    val systemPrompt: String? = null,
    val chatFormat: String? = null,
    val threadCount: Int? = null,
    val gpuLayers: Int? = null,
    val backend: String? = null
)