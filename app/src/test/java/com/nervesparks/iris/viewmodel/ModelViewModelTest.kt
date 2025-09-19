package com.nervesparks.iris.viewmodel

import android.llama.cpp.LLamaAndroid
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.data.repository.ModelConfiguration
import com.nervesparks.iris.data.repository.ModelRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ModelViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var llamaAndroid: LLamaAndroid
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        llamaAndroid = mockk(relaxed = true) {
            every { getAvailableBackends() } returns "cpu"
            every { isAdrenoGpu() } returns false
        }
        userPreferencesRepository = mockk(relaxed = true) {
            every { modelTemperature } returns 0.7f
            every { modelTopP } returns 0.9f
            every { modelTopK } returns 40
            every { modelMaxTokens } returns 2048
            every { modelContextLength } returns 32768
            every { modelSystemPrompt } returns "system"
            every { modelChatFormat } returns "CHATML"
            every { modelThreadCount } returns 4
            every { modelGpuLayers } returns -1
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getAvailableModels surfaces repository data`() = runTest {
        val expectedModels = listOf(
            mapOf(
                "name" to "test-model.gguf",
                "source" to "https://example.com/test-model.gguf",
                "destination" to "test-model.gguf",
                "supportsReasoning" to "false"
            )
        )
        val repository = FakeModelRepository(expectedModels)

        val viewModel = ModelViewModel(llamaAndroid, userPreferencesRepository, repository)
        val directory = File(".")

        val result = viewModel.getAvailableModels(directory)

        assertEquals(expectedModels, result)
        assertEquals(expectedModels, viewModel.availableModels)
    }

    private class FakeModelRepository(
        private val models: List<Map<String, String>>
    ) : ModelRepository {
        override suspend fun getAvailableModels(directory: File): List<Map<String, String>> = models

        override suspend fun refreshAvailableModels(): List<Map<String, String>> = throw UnsupportedOperationException()

        override suspend fun loadModel(modelPath: String): Result<Unit> = throw UnsupportedOperationException()

        override suspend fun loadModelByName(modelName: String, directory: File): Result<Unit> = throw UnsupportedOperationException()

        override suspend fun getLoadedModelName(): String = throw UnsupportedOperationException()

        override suspend fun setLoadedModelName(modelName: String) = throw UnsupportedOperationException()

        override suspend fun getModelConfiguration(modelName: String): ModelConfiguration = throw UnsupportedOperationException()

        override suspend fun saveModelConfiguration(modelName: String, config: ModelConfiguration) = throw UnsupportedOperationException()

        override suspend fun modelExists(modelName: String, directory: File): Boolean = throw UnsupportedOperationException()

        override suspend fun getModelFileSize(modelName: String, directory: File): Long = throw UnsupportedOperationException()

        override suspend fun deleteModel(modelName: String, directory: File): Result<Unit> = throw UnsupportedOperationException()
    }
}
