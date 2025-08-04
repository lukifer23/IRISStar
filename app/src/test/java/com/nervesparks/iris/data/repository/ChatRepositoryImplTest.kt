package com.nervesparks.iris.data.repository

import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.nervesparks.iris.data.db.AppDatabase
import com.nervesparks.iris.data.db.ChatDao
import com.nervesparks.iris.data.repository.impl.ChatRepositoryImpl
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class ChatRepositoryImplTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: ChatDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        dao = db.chatDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun createAndRetrieveChat_success() = runBlocking {
        val repo = ChatRepositoryImpl(dao)
        val id = repo.createChat("Test Chat")
        val chat = repo.getChat(id)
        assertEquals("Test Chat", chat?.title)
    }

    @Test(expected = RuntimeException::class)
    fun createChat_failureThrows() = runBlocking {
        val failingDao = mockk<ChatDao>()
        coEvery { failingDao.insertChat(any()) } throws RuntimeException("db error")
        val repo = ChatRepositoryImpl(failingDao)
        repo.createChat("Fail")
    }
}
