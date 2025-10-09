package com.nervesparks.iris.viewmodel

import timber.log.Timber
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nervesparks.iris.data.repository.ChatRepository
import com.nervesparks.iris.data.DocumentRepository
import com.nervesparks.iris.data.db.Chat
import com.nervesparks.iris.data.db.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * PHASE 1.1: ChatViewModel - Extracted from MainViewModel
 * Handles chat management, messaging, and persistence
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val documentRepository: DocumentRepository
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

            if (text.isBlank()) {
                withContext(Dispatchers.Main) {
                    isDocumentIndexing = false
                    documentIndexingError = "Document is empty"
                }
                return@launch
            }

            try {
                // Note: embedText functionality is still in MainViewModel
                // This would need to be moved to a service or the embedding logic needs refactoring
                // For now, we'll mark as not implemented
                Timber.tag(tag).d("Document indexing requires embedText functionality from MainViewModel")
                withContext(Dispatchers.Main) {
                    documentIndexingError = "Document indexing not yet fully implemented"
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

    // Thinking tokens settings
    fun updateShowThinkingTokens(show: Boolean) {
        showThinkingTokens = show
        Timber.tag(tag).d("Show thinking tokens: $show")
    }

    fun updateThinkingTokenStyle(style: String) {
        thinkingTokenStyle = style
        Timber.tag(tag).d("Thinking token style: $style")
    }

    fun updateSupportsReasoning(supports: Boolean) {
        supportsReasoning = supports
        Timber.tag(tag).d("Supports reasoning: $supports")
    }
}
