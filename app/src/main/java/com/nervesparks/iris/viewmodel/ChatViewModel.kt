package com.nervesparks.iris.viewmodel

import android.util.Log
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
import kotlinx.coroutines.launch
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

    // Current chat state
    private var currentChat: Chat? = null

    // UI State
    var messages by mutableStateOf(mutableStateListOf<Map<String, Any>>())
    var showThinkingTokens by mutableStateOf(true)
    var thinkingTokenStyle by mutableStateOf("COLLAPSIBLE")
    var supportsReasoning by mutableStateOf(false)

    // Chat management functions
    fun renameChat(chat: Chat, title: String) {
        viewModelScope.launch {
            try {
                chatRepository.updateChatTitle(chat.id, title)
                Log.d(tag, "Chat renamed: ${chat.id} -> $title")
            } catch (e: Exception) {
                Log.e(tag, "Error renaming chat", e)
            }
        }
    }

    fun deleteChat(chat: Chat) {
        viewModelScope.launch {
            try {
                chatRepository.deleteChat(chat)
                Log.d(tag, "Chat deleted: ${chat.id}")
            } catch (e: Exception) {
                Log.e(tag, "Error deleting chat", e)
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
                    Log.d(tag, "Loaded chat: $chatId with ${messages.size} messages")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading chat: $chatId", e)
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
                Log.d(tag, "Chat saved: $chatId")
            } catch (e: Exception) {
                Log.e(tag, "Error saving chat", e)
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
        Log.d(tag, "Chat cleared")
    }

    fun addMessage(role: String, content: String) {
        val message = mapOf(
            "role" to role,
            "content" to content,
            "timestamp" to System.currentTimeMillis()
        )
        messages.add(message)
        Log.d(tag, "Message added: $role")
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
            try {
                // TODO: Implement document indexing
                Log.d(tag, "Document indexing not yet implemented")
            } catch (e: Exception) {
                Log.e(tag, "Error indexing document", e)
            }
        }
    }

    // Thinking tokens settings
    fun updateShowThinkingTokens(show: Boolean) {
        showThinkingTokens = show
        Log.d(tag, "Show thinking tokens: $show")
    }

    fun updateThinkingTokenStyle(style: String) {
        thinkingTokenStyle = style
        Log.d(tag, "Thinking token style: $style")
    }

    fun updateSupportsReasoning(supports: Boolean) {
        supportsReasoning = supports
        Log.d(tag, "Supports reasoning: $supports")
    }
}
