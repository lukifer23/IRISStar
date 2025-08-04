package com.nervesparks.iris

import android.app.Application
import android.llama.cpp.LLamaAndroid
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nervesparks.iris.data.ChatRepository
import com.nervesparks.iris.data.HuggingFaceApiService
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.data.db.AppDatabase
import com.nervesparks.iris.data.repository.ModelInfo
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var db: AppDatabase
    private lateinit var chatRepository: ChatRepository
    private lateinit var userPrefs: UserPreferencesRepository
    private lateinit var huggingFace: HuggingFaceApiService

    @Before
    fun setup() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
        application = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(application, AppDatabase::class.java).allowMainThreadQueries().build()
        chatRepository = ChatRepository(db.chatDao())
        userPrefs = UserPreferencesRepository.getInstance(application)
        huggingFace = object : HuggingFaceApiService {
            override suspend fun searchModels(query: String, token: String?) = emptyList<ModelInfo>()
            override suspend fun getModelDetails(modelId: String, token: String?) = ModelInfo("id","name",0,0, emptyList())
        }
    }

    @After
    fun tearDown() {
        db.close()
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun send_addsAssistantMessage() = runTest(dispatcher) {
        val llama = mockk<LLamaAndroid>()
        coEvery { llama.countTokens(any()) } returns 1
        every { llama.getIsMarked() } returns false
        every { llama.getIsCompleteEOT() } returns true
        coEvery { llama.send(any()) } returns flowOf("Hi")
        val vm = MainViewModel(llama, userPrefs, chatRepository, huggingFace, application)
        vm.message = "Hello"
        vm.send()
        this.advanceUntilIdle()
        assertTrue(vm.messages.any { it["role"] == "assistant" && it["content"] == "Hi" })
    }

    @Test
    fun send_handlesError() = runTest(dispatcher) {
        val llama = mockk<LLamaAndroid>()
        coEvery { llama.countTokens(any()) } returns 1
        every { llama.getIsMarked() } returns false
        every { llama.getIsCompleteEOT() } returns true
        coEvery { llama.send(any()) } returns flow { throw RuntimeException("boom") }
        val vm = MainViewModel(llama, userPrefs, chatRepository, huggingFace, application)
        vm.message = "Hello"
        vm.send()
        this.advanceUntilIdle()
        assertTrue(vm.messages.any { it["role"] == "error" })
    }

    @Test
    fun loadModelByName_updatesState() = runTest(dispatcher) {
        val llama = mockk<LLamaAndroid>()
        coEvery { llama.unload() } justRun
        coEvery { llama.load(any(), any(), any(), any(), any()) } justRun
        every { llama.send_eot_str() } returns "eot"
        val vm = MainViewModel(llama, userPrefs, chatRepository, huggingFace, application)
        val dir = File(application.filesDir, "models").apply { mkdirs() }
        val modelFile = File(dir, "test.gguf").apply { writeText("data") }
        vm.loadModelByName(modelFile.name, dir)
        this.advanceUntilIdle()
        assertEquals(modelFile.name, vm.loadedModelName.value)
    }
}
