package com.nervesparks.iris.viewmodel

import timber.log.Timber
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nervesparks.iris.data.DocumentRepository
import com.nervesparks.iris.data.exceptions.ErrorHandler
import com.nervesparks.iris.data.exceptions.ValidationException
import com.nervesparks.iris.data.repository.ChatRepository
import com.nervesparks.iris.data.db.Chat
import com.nervesparks.iris.data.db.Message
import com.nervesparks.iris.llm.EmbeddingService
import com.nervesparks.iris.llm.performDocumentIndexing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Data class for chat-specific settings
 */
data class ChatSettings(
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

/**
 * PHASE 1.1: ChatViewModel - Extracted from MainViewModel
 * Handles chat management, messaging, and persistence
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val documentRepository: DocumentRepository,
    private val embeddingService: EmbeddingService
) : ViewModel() {

    private val tag = "ChatViewModel"

    // Chat list from repository
    val chats = chatRepository.observeChats()
    val chatStats = chatRepository.observeChatStats()

    // Current chat state
    private var currentChat: Chat? = null

    // UI State
    var messages by mutableStateOf(mutableStateListOf<Map<String, Any>>())
    var showThinkingTokens by mutableStateOf(true)
    var thinkingTokenStyle by mutableStateOf("COLLAPSIBLE")
    var supportsReasoning by mutableStateOf(false)

    // Document indexing state
    var isDocumentIndexing by mutableStateOf(false)
        private set

    var documentIndexingError by mutableStateOf<String?>(null)
        private set

    var documentIndexingSuccess by mutableStateOf<String?>(null)
        private set

    // Chat management functions
    fun renameChat(chat: Chat, title: String) {
        viewModelScope.launch {
            try {
                chatRepository.updateChatTitle(chat.id, title)
                Timber.tag(tag).d("Chat renamed: ${chat.id} -> $title")
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error renaming chat")
                ErrorHandler.reportError(e, "Chat Rename", ErrorHandler.ErrorSeverity.LOW, "Failed to rename chat. Please try again.")
            }
        }
    }

    fun deleteChat(chat: Chat) {
        viewModelScope.launch {
            try {
                chatRepository.deleteChat(chat)
                Timber.tag(tag).d("Chat deleted: ${chat.id}")
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error deleting chat")
                ErrorHandler.reportError(e, "Chat Delete", ErrorHandler.ErrorSeverity.MEDIUM, "Failed to delete chat. Please try again.")
            }
        }
    }

    fun loadChat(chatId: Long) {
        viewModelScope.launch {
            try {
                currentChat = chatRepository.getChat(chatId)
                if (currentChat != null) {
                    val chatMessages = chatRepository.getMessages(chatId)
                    messages.clear()
                    chatMessages.forEach { message ->
                        messages.add(mapOf(
                            "role" to message.role,
                            "content" to message.content,
                            "timestamp" to message.timestamp
                        ))
                    }
                    Timber.tag(tag).d("Loaded chat: $chatId with ${messages.size} messages")
                }
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error loading chat: $chatId")
                ErrorHandler.reportError(e, "Chat Load", ErrorHandler.ErrorSeverity.MEDIUM, "Failed to load chat. Please try again.")
            }
        }
    }

    fun saveCurrentChat(title: String? = null) {
        viewModelScope.launch {
            try {
                val chatTitle = title ?: generateChatTitle()
                val chatId = chatRepository.createChat(chatTitle)

                messages.forEachIndexed { index, message ->
                    chatRepository.addMessage(
                        chatId = chatId,
                        role = message["role"] as? String ?: "user",
                        content = message["content"] as? String ?: "",
                        index = index
                    )
                }

                currentChat = chatRepository.getChat(chatId)
                Timber.tag(tag).d("Chat saved: $chatId")
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error saving chat")
                ErrorHandler.reportError(e, "Chat Save", ErrorHandler.ErrorSeverity.HIGH, "Failed to save chat. Please try again.")
            }
        }
    }

    private fun generateChatTitle(): String {
        val firstMessage = messages.firstOrNull { it["role"] == "user" }
        val content = firstMessage?.get("content") as? String ?: ""
        return if (content.length > 50) {
            content.take(47) + "..."
        } else if (content.isNotBlank()) {
            content
        } else {
            "New Chat"
        }
    }

    fun clearChat() {
        messages.clear()
        currentChat = null
        Timber.tag(tag).d("Chat cleared")
    }

    fun addMessage(role: String, content: String) {
        val message = mapOf(
            "role" to role,
            "content" to content,
            "timestamp" to System.currentTimeMillis()
        )
        messages.add(message)
        Timber.tag(tag).d("Message added: $role")
    }

    // Thinking token settings
    fun updateShowThinkingTokens(show: Boolean) {
        viewModelScope.launch {
            try {
                // Update local state
                showThinkingTokens = show
                Timber.tag(tag).d("Show thinking tokens updated: $show")
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error updating show thinking tokens")
                ErrorHandler.reportError(e, "Thinking Tokens Update", ErrorHandler.ErrorSeverity.LOW, "Failed to update thinking tokens setting.")
            }
        }
    }

    fun updateThinkingTokenStyle(style: String) {
        viewModelScope.launch {
            try {
                // Update local state
                thinkingTokenStyle = style
                Timber.tag(tag).d("Thinking token style updated: $style")
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error updating thinking token style")
                ErrorHandler.reportError(e, "Thinking Token Style Update", ErrorHandler.ErrorSeverity.LOW, "Failed to update thinking token style.")
            }
        }
    }

    // Per-chat settings management
    suspend fun updateChatSettings(
        chatId: Long,
        modelName: String? = null,
        temperature: Float? = null,
        topP: Float? = null,
        topK: Int? = null,
        maxTokens: Int? = null,
        contextLength: Int? = null,
        systemPrompt: String? = null,
        chatFormat: String? = null,
        threadCount: Int? = null,
        gpuLayers: Int? = null,
        backend: String? = null
    ) {
        try {
            chatRepository.updateChatSettings(
                chatId = chatId,
                modelName = modelName,
                temperature = temperature,
                topP = topP,
                topK = topK,
                maxTokens = maxTokens,
                contextLength = contextLength,
                systemPrompt = systemPrompt,
                chatFormat = chatFormat,
                threadCount = threadCount,
                gpuLayers = gpuLayers,
                backend = backend
            )
            Timber.tag(tag).d("Chat settings updated for chat: $chatId")
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error updating chat settings")
            ErrorHandler.reportError(e, "Chat Settings Update", ErrorHandler.ErrorSeverity.MEDIUM, "Failed to update chat settings. Please try again.")
        }
    }

    // Get current chat settings
    suspend fun getChatSettings(chatId: Long): ChatSettings {
        return try {
            val chat = chatRepository.getChat(chatId)
            ChatSettings(
                modelName = chat?.modelName,
                temperature = chat?.temperature,
                topP = chat?.topP,
                topK = chat?.topK,
                maxTokens = chat?.maxTokens,
                contextLength = chat?.contextLength,
                systemPrompt = chat?.systemPrompt,
                chatFormat = chat?.chatFormat,
                threadCount = chat?.threadCount,
                gpuLayers = chat?.gpuLayers,
                backend = chat?.backend
            )
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error getting chat settings")
            ChatSettings() // Return defaults
        }
    }

    fun updateLastMessage(content: String) {
        if (messages.isNotEmpty()) {
            val lastMessage = messages.last().toMutableMap()
            lastMessage["content"] = content
            messages[messages.lastIndex] = lastMessage
        }
    }

    fun indexDocument(text: String) {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                isDocumentIndexing = true
                documentIndexingError = null
                documentIndexingSuccess = null
            }

            try {
                performDocumentIndexing(text, embeddingService, documentRepository)
                withContext(Dispatchers.Main) {
                    documentIndexingSuccess = "Document indexed successfully"
                }
            } catch (e: ValidationException) {
                Timber.tag(tag).e(e, "Validation error indexing document")
                withContext(Dispatchers.Main) {
                    documentIndexingError = e.message ?: "Failed to index document"
                }
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error indexing document")
                withContext(Dispatchers.Main) {
                    documentIndexingError = e.message ?: "Failed to index document"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isDocumentIndexing = false
                }
            }
        }
    }


    fun updateSupportsReasoning(supports: Boolean) {
        supportsReasoning = supports
        Timber.tag(tag).d("Supports reasoning: $supports")
    }

    /**
     * Enhanced cleanup with memory optimization
     */
    fun cleanup() {
        // Clear messages if they are too many to prevent memory issues
        if (messages.size > 100) {
            // Keep only recent messages (last 50)
            messages.clear()
            messages.addAll(messages.takeLast(50))
        }

        Timber.tag(tag).d("ChatViewModel cleaned up")
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}
