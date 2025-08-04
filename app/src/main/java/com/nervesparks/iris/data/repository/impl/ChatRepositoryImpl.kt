package com.nervesparks.iris.data.repository.impl

import com.nervesparks.iris.data.db.Chat
import com.nervesparks.iris.data.db.ChatDao
import com.nervesparks.iris.data.db.Message
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

    override fun observeChat(chatId: Long): Flow<Chat> = chatDao.observeChat(chatId)

    override suspend fun createChat(title: String): Long = withContext(Dispatchers.IO) {
        val chat = Chat(title = title)
        chatDao.insertChat(chat)
    }

    override suspend fun updateChatTitle(chatId: Long, title: String) = withContext(Dispatchers.IO) {
        val chat = chatDao.getChat(chatId)
        if (chat != null) {
            chatDao.updateChat(chat.copy(title = title, updated = System.currentTimeMillis()))
        }
    }

    override suspend fun deleteChat(chatId: Long) = withContext(Dispatchers.IO) {
        val chat = chatDao.getChat(chatId)
        if (chat != null) {
            chatDao.deleteChat(chat)
        }
    }

    override suspend fun getMessages(chatId: Long): List<Message> = withContext(Dispatchers.IO) {
        chatDao.loadMessages(chatId)
    }

    override suspend fun loadMessages(chatId: Long): List<Message> = withContext(Dispatchers.IO) {
        chatDao.loadMessages(chatId)
    }

    override suspend fun addMessage(chatId: Long, role: String, content: String, index: Int) = withContext(Dispatchers.IO) {
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

    override suspend fun getChatStats(chatId: Long): ChatStats {
        // Not implemented
        return ChatStats(0, 0, 0, 0, 0)
    }
}

 