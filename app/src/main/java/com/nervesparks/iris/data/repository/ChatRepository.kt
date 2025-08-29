package com.nervesparks.iris.data.repository

import com.nervesparks.iris.data.db.Chat
import com.nervesparks.iris.data.db.Message
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for chat operations
 */
interface ChatRepository {
    
    /**
     * Get all chats
     */
    suspend fun getAllChats(): List<Chat>
    
    /**
     * Get chat by ID
     */
    suspend fun getChat(chatId: Long): Chat?

    /**
     * Observe all chats
     */
    fun observeChats(): Flow<List<Chat>>

    /**
     * Observe chat changes
     */
    fun observeChat(chatId: Long): Flow<Chat>
    
    /**
     * Create a new chat
     */
    suspend fun createChat(title: String = "New Chat"): Long
    
    /**
     * Update chat title
     */
    suspend fun updateChatTitle(chatId: Long, title: String)

    /**
     * Rename chat using entity
     */
    suspend fun renameChat(chat: Chat, newTitle: String)
    
    /**
     * Delete a chat
     */
    suspend fun deleteChat(chatId: Long)

    /**
     * Delete chat using entity
     */
    suspend fun deleteChat(chat: Chat)
    
    /**
     * Get messages for a chat
     */
    suspend fun getMessages(chatId: Long): List<Message>
    
    /**
     * Load messages for a chat
     */
    suspend fun loadMessages(chatId: Long): List<Message>
    
    /**
     * Add a message to a chat
     */
    suspend fun addMessage(chatId: Long, role: String, content: String, index: Int = 0)
    
    /**
     * Update a message
     */
    suspend fun updateMessage(messageId: Long, content: String)
    
    /**
     * Delete a message
     */
    suspend fun deleteMessage(messageId: Long)
    
    /**
     * Clear all messages in a chat
     */
    suspend fun clearMessages(chatId: Long)

    /**
     * Save chat with associated messages
     */
    suspend fun saveChatWithMessages(chat: Chat, messages: List<Message>): Long
    
    /**
     * Get chat statistics
     */
    suspend fun getChatStats(chatId: Long): ChatStats
}

/**
 * Data class for chat statistics
 */
data class ChatStats(
    val totalMessages: Int,
    val userMessages: Int,
    val assistantMessages: Int,
    val totalTokens: Int,
    val averageResponseTime: Long
) 