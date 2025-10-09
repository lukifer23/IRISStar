package com.nervesparks.iris.data.repository.impl

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nervesparks.iris.data.db.AppDatabase
import com.nervesparks.iris.data.db.Message
import com.nervesparks.iris.data.exceptions.ValidationException
import com.nervesparks.iris.data.repository.ChatRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChatRepositoryImplTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ChatRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ChatRepositoryImpl(db.chatDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun createChatAndRetrieve() = runBlocking {
        val id = repository.createChat("My Chat")
        val chat = repository.getChat(id)
        assertEquals("My Chat", chat?.title)
    }

    @Test
    fun addMessagePersists() = runBlocking {
        val chatId = repository.createChat("Chat")
        repository.addMessage(chatId, "user", "hello", 0)
        val messages = repository.getMessages(chatId)
        assertEquals(1, messages.size)
        assertEquals("hello", messages.first().content)
    }

    @Test
    fun updateChatTitleWithInvalidIdThrows() {
        assertThrows(ValidationException::class.java) {
            runBlocking { repository.updateChatTitle(0, "New") }
        }
    }

    @Test
    fun getChatStatsReturnsAggregatedMetrics() = runBlocking {
        val chatId = repository.createChat("Stats Chat")
        val dao = db.chatDao()
        dao.insertMessage(
            Message(chatId = chatId, role = "user", content = "Hello there", timestamp = 1_000L, index = 0)
        )
        dao.insertMessage(
            Message(chatId = chatId, role = "assistant", content = "Hi!", timestamp = 3_000L, index = 1)
        )
        dao.insertMessage(
            Message(chatId = chatId, role = "user", content = "How are you?", timestamp = 7_000L, index = 2)
        )

        val stats = repository.getChatStats(chatId)

        assertEquals(3, stats.totalMessages)
        assertEquals(2, stats.userMessages)
        assertEquals(1, stats.assistantMessages)
        assertEquals(7, stats.totalTokens)
        assertEquals(3_000L, stats.averageResponseTime)
    }

    @Test
    fun observeChatStatsReflectsMessageCounts() = runBlocking {
        val chatIdOne = repository.createChat("First")
        val chatIdTwo = repository.createChat("Second")
        val dao = db.chatDao()
        dao.insertMessage(
            Message(chatId = chatIdOne, role = "user", content = "Hello", timestamp = 500L, index = 0)
        )
        dao.insertMessage(
            Message(chatId = chatIdTwo, role = "user", content = "Hi", timestamp = 600L, index = 0)
        )
        dao.insertMessage(
            Message(chatId = chatIdTwo, role = "assistant", content = "Howdy", timestamp = 700L, index = 1)
        )

        val statsMap = repository.observeChatStats().first()

        assertEquals(1, statsMap[chatIdOne]?.totalMessages)
        assertEquals(2, statsMap[chatIdTwo]?.totalMessages)
        assertTrue((statsMap[chatIdTwo]?.assistantMessages ?: 0) > 0)
    }
}

