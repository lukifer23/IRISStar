package com.nervesparks.iris.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nervesparks.iris.data.db.AppDatabase
import com.nervesparks.iris.data.repository.impl.ChatRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

class ChatFlowIntegrationTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: ChatRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = ChatRepositoryImpl(db.chatDao())
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun chatFlow_addAndLoadMessages() = runBlocking {
        val chatId = repo.createChat("Title")
        repo.addMessage(chatId, "user", "Hello", 0)
        val messages = repo.getMessages(chatId)
        assertEquals(1, messages.size)
        assertEquals("Hello", messages[0].content)
    }
}
