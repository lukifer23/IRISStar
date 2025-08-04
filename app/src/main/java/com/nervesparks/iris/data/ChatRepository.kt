package com.nervesparks.iris.data

import android.content.Context
import androidx.room.Room
import com.nervesparks.iris.data.db.AppDatabase
import com.nervesparks.iris.data.db.Chat
import com.nervesparks.iris.data.db.ChatDao
import com.nervesparks.iris.data.db.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ChatRepository @Inject constructor(private val dao: ChatDao) {

    fun observeChats(): Flow<List<Chat>> = dao.observeChats()

    fun observeChat(id: Long): Flow<Chat> = dao.observeChat(id)

    suspend fun loadMessages(chatId: Long): List<Message> = withContext(Dispatchers.IO) { dao.loadMessages(chatId) }

    suspend fun deleteChat(chat: Chat) = withContext(Dispatchers.IO) { dao.deleteChat(chat) }

    suspend fun renameChat(chat: Chat, newTitle: String) = withContext(Dispatchers.IO) {
        dao.updateChat(chat.copy(title = newTitle, updated = System.currentTimeMillis()))
    }

    suspend fun saveChatWithMessages(chat: Chat, messages: List<Message>): Long {
        return withContext(Dispatchers.IO) {
            val id = if (chat.id == 0L) {
                dao.insertChat(chat)
            } else {
                dao.updateChat(chat)
                chat.id
            }
            val msgs = messages.mapIndexed { idx, m ->
                m.copy(chatId = id, index = idx)
            }
            dao.deleteMessages(id)
            dao.insertMessages(msgs)
            id
        }
    }

    
}