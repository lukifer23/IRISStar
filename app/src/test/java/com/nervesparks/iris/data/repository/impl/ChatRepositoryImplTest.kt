package com.nervesparks.iris.data.repository.impl

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nervesparks.iris.data.db.AppDatabase
import com.nervesparks.iris.data.exceptions.ValidationException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChatRepositoryImplTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ChatRepositoryImpl

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
}

