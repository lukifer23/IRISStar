package com.nervesparks.iris.data.repository.impl

import com.nervesparks.iris.data.db.Chat
import com.nervesparks.iris.data.db.ChatDao
import com.nervesparks.iris.data.db.Message
import com.nervesparks.iris.data.exceptions.ValidationException
import com.nervesparks.iris.data.repository.ChatRepository
import com.nervesparks.iris.data.repository.ChatStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao
) : ChatRepository {

    override suspend fun getAllChats(): List<Chat> = withContext(Dispatchers.IO) {
        chatDao.getAllChats()
    }

    override suspend fun getChat(chatId: Long): Chat? = withContext(Dispatchers.IO) {
        chatDao.getChat(chatId)
    }

    override fun observeChats(): Flow<List<Chat>> = chatDao.observeChats()

    override fun observeChat(chatId: Long): Flow<Chat> = chatDao.observeChat(chatId)

    override suspend fun createChat(title: String): Long = withContext(Dispatchers.IO) {
        // Validate input parameters
        if (title.isBlank()) {
            throw ValidationException("Chat title cannot be blank")
        }
        
        if (title.length > 100) {
            throw ValidationException("Chat title too long (max 100 characters)")
        }
        
        val chat = Chat(title = title)
        chatDao.insertChat(chat)
    }

    override suspend fun updateChatTitle(chatId: Long, title: String) = withContext(Dispatchers.IO) {
        // Validate input parameters
        if (chatId <= 0) {
            throw ValidationException("Invalid chat ID: $chatId")
        }
        
        if (title.isBlank()) {
            throw ValidationException("Chat title cannot be blank")
        }
        
        if (title.length > 100) {
            throw ValidationException("Chat title too long (max 100 characters)")
        }
        
        val chat = chatDao.getChat(chatId)
        if (chat != null) {
            chatDao.updateChat(chat.copy(title = title, updated = System.currentTimeMillis()))
        }
    }

    override suspend fun renameChat(chat: Chat, newTitle: String) = withContext(Dispatchers.IO) {
        chatDao.updateChat(chat.copy(title = newTitle, updated = System.currentTimeMillis()))
    }

    override suspend fun deleteChat(chatId: Long) = withContext(Dispatchers.IO) {
        val chat = chatDao.getChat(chatId)
        if (chat != null) {
            chatDao.deleteChat(chat)
        }
    }

    override suspend fun deleteChat(chat: Chat) = withContext(Dispatchers.IO) {
        chatDao.deleteChat(chat)
    }

    override suspend fun getMessages(chatId: Long): List<Message> = withContext(Dispatchers.IO) {
        chatDao.loadMessages(chatId)
    }

    override suspend fun loadMessages(chatId: Long): List<Message> = withContext(Dispatchers.IO) {
        chatDao.loadMessages(chatId)
    }

    override suspend fun addMessage(chatId: Long, role: String, content: String, index: Int) = withContext(Dispatchers.IO) {
        // Validate input parameters
        if (chatId <= 0) {
            throw ValidationException("Invalid chat ID: $chatId")
        }
        
        if (role.isBlank()) {
            throw ValidationException("Message role cannot be blank")
        }
        
        if (!listOf("user", "assistant", "system", "error").contains(role.lowercase())) {
            throw ValidationException("Invalid message role: $role")
        }
        
        if (content.isBlank()) {
            throw ValidationException("Message content cannot be blank")
        }
        
        if (index < 0) {
            throw ValidationException("Invalid message index: $index")
        }
        
        val message = Message(chatId = chatId, role = role, content = content, index = index)
        chatDao.insertMessage(message)
    }

    override suspend fun updateMessage(messageId: Long, content: String) = withContext(Dispatchers.IO) {
        val message = chatDao.getMessage(messageId)
        if (message != null) {
            chatDao.updateMessage(message.copy(content = content))
        }
    }

    override suspend fun deleteMessage(messageId: Long) = withContext(Dispatchers.IO) {
        val message = chatDao.getMessage(messageId)
        if (message != null) {
            chatDao.deleteMessage(message)
        }
    }

    override suspend fun clearMessages(chatId: Long) = withContext(Dispatchers.IO) {
        chatDao.deleteMessages(chatId)
    }

    override suspend fun saveChatWithMessages(chat: Chat, messages: List<Message>): Long {
        return withContext(Dispatchers.IO) {
            val id = if (chat.id == 0L) {
                chatDao.insertChat(chat)
            } else {
                chatDao.updateChat(chat)
                chat.id
            }
            val msgs = messages.mapIndexed { idx, m ->
                m.copy(chatId = id, index = idx)
            }
            chatDao.deleteMessages(id)
            chatDao.insertMessages(msgs)
            id
        }
    }

    override suspend fun getChatStats(chatId: Long): ChatStats {
        // Not implemented
        return ChatStats(0, 0, 0, 0, 0)
    }
}

 