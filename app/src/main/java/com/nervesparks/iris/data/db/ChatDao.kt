package com.nervesparks.iris.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: Chat): Long

    @Update
    suspend fun updateChat(chat: Chat)

    @Delete
    suspend fun deleteChat(chat: Chat)

    @Query("SELECT * FROM chats ORDER BY updated DESC")
    fun observeChats(): Flow<List<Chat>>

    @Query("SELECT * FROM chats")
    suspend fun getAllChats(): List<Chat>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    fun observeChat(chatId: Long): Flow<Chat>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChat(chatId: Long): Chat?

    @Query("UPDATE chats SET modelName = :modelName, temperature = :temperature, topP = :topP, topK = :topK, maxTokens = :maxTokens, contextLength = :contextLength, systemPrompt = :systemPrompt, chatFormat = :chatFormat, threadCount = :threadCount, gpuLayers = :gpuLayers, backend = :backend, updated = :updated WHERE id = :chatId")
    suspend fun updateChatSettings(
        chatId: Long,
        modelName: String?,
        temperature: Float?,
        topP: Float?,
        topK: Int?,
        maxTokens: Int?,
        contextLength: Int?,
        systemPrompt: String?,
        chatFormat: String?,
        threadCount: Int?,
        gpuLayers: Int?,
        backend: String?,
        updated: Long = System.currentTimeMillis()
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<Message>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Update
    suspend fun updateMessage(message: Message)

    @Delete
    suspend fun deleteMessage(message: Message)

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessage(messageId: Long): Message?

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY `index`")
    suspend fun loadMessages(chatId: Long): List<Message>

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessages(chatId: Long)

    @Query(
        """
        SELECT 
            :chatId AS chatId,
            COUNT(*) AS totalMessages,
            IFNULL(SUM(CASE WHEN LOWER(role) = 'user' THEN 1 ELSE 0 END), 0) AS userMessages,
            IFNULL(SUM(CASE WHEN LOWER(role) = 'assistant' THEN 1 ELSE 0 END), 0) AS assistantMessages,
            IFNULL(SUM(LENGTH(content)), 0) AS totalCharacters,
            MIN(timestamp) AS firstTimestamp,
            MAX(timestamp) AS lastTimestamp
        FROM messages
        WHERE chatId = :chatId
        """
    )
    suspend fun getChatAggregate(chatId: Long): ChatAggregate?

    @Query(
        """
        SELECT 
            chatId AS chatId,
            COUNT(*) AS totalMessages,
            IFNULL(SUM(CASE WHEN LOWER(role) = 'user' THEN 1 ELSE 0 END), 0) AS userMessages,
            IFNULL(SUM(CASE WHEN LOWER(role) = 'assistant' THEN 1 ELSE 0 END), 0) AS assistantMessages,
            IFNULL(SUM(LENGTH(content)), 0) AS totalCharacters,
            MIN(timestamp) AS firstTimestamp,
            MAX(timestamp) AS lastTimestamp
        FROM messages
        GROUP BY chatId
        """
    )
    fun observeChatAggregates(): Flow<List<ChatAggregate>>
}

data class ChatAggregate(
    val chatId: Long,
    val totalMessages: Long,
    val userMessages: Long,
    val assistantMessages: Long,
    val totalCharacters: Long,
    val firstTimestamp: Long?,
    val lastTimestamp: Long?
)
