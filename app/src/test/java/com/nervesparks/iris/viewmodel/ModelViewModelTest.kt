package com.nervesparks.iris.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nervesparks.iris.data.FakeUserPreferencesRepository
import com.nervesparks.iris.data.repository.ModelConfiguration
import com.nervesparks.iris.data.repository.ModelRepository
import com.nervesparks.iris.llm.ModelLoader
import com.nervesparks.iris.llm.ModelPerformanceTracker
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.llama.cpp.LLamaAndroid
import java.io.File

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ModelViewModelTest {

    private lateinit var context: Context
    private lateinit var dispatcher: StandardTestDispatcher
    private lateinit var fakeRepository: FakeModelRepository
    private lateinit var userPreferencesRepository: FakeUserPreferencesRepository
    private lateinit var llamaAndroid: LLamaAndroid
    private lateinit var modelLoader: ModelLoader
    private lateinit var performanceTracker: ModelPerformanceTracker

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        fakeRepository = FakeModelRepository()
        userPreferencesRepository = FakeUserPreferencesRepository(context)
        llamaAndroid = mockk(relaxed = true) {
            every { getAvailableBackends() } returns "cpu"
            every { isAdrenoGpu() } returns false
        }
        modelLoader = mockk(relaxed = true)
        performanceTracker = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadExistingModelsUpdatesAvailableModels() = runTest {
        val viewModel = createViewModel()
        val expected = listOf(
            mapOf("name" to "model1.gguf", "source" to "local", "destination" to "model1.gguf")
        )
        fakeRepository.availableModelsResponse = expected
        val directory = createTempDir()

        viewModel.loadExistingModels(directory)
        advanceUntilIdle()

        assertEquals(expected, viewModel.availableModels)
    }

    @Test
    fun loadExistingModelsPreservesExistingStateOnError() = runTest {
        val viewModel = createViewModel()
        viewModel.availableModels = listOf(mapOf("name" to "cached.gguf"))
        fakeRepository.shouldThrow = true
        val directory = createTempDir()

        viewModel.loadExistingModels(directory)
        advanceUntilIdle()

        assertEquals(listOf(mapOf("name" to "cached.gguf")), viewModel.availableModels)
    }

    @Test
    fun cachedModelsSurviveNewViewModelInstance() = runTest {
        val expected = listOf(mapOf("name" to "model-cache.gguf"))
        fakeRepository.availableModelsResponse = expected
        val directory = createTempDir()

        val firstViewModel = createViewModel()
        firstViewModel.loadExistingModels(directory)
        advanceUntilIdle()
        assertEquals(expected, firstViewModel.availableModels)

        fakeRepository.availableModelsResponse = emptyList()
        val secondViewModel = createViewModel()
        secondViewModel.loadExistingModels(directory)
        advanceUntilIdle()

        assertEquals(expected, secondViewModel.availableModels)
    }

    private fun createViewModel(): ModelViewModel {
        return ModelViewModel(
            llamaAndroid = llamaAndroid,
            modelLoader = modelLoader,
            performanceTracker = performanceTracker,
            userPreferencesRepository = userPreferencesRepository,
            modelRepository = fakeRepository
        )
    }

    private fun createTempDir(): File {
        return kotlin.io.path.createTempDirectory().toFile()
    }

    private class FakeModelRepository : ModelRepository {
        var availableModelsResponse: List<Map<String, String>> = emptyList()
        var defaultModelsResponse: List<Map<String, String>> = emptyList()
        var shouldThrow: Boolean = false
        private var cached: List<Map<String, String>> = emptyList()

        override suspend fun getAvailableModels(directory: File): List<Map<String, String>> {
            if (shouldThrow) throw IllegalStateException("test error")
            if (availableModelsResponse.isNotEmpty()) {
                cached = availableModelsResponse
                return availableModelsResponse
            }
            return cached
        }

        override suspend fun refreshAvailableModels(): List<Map<String, String>> = emptyList()

        override suspend fun loadModel(modelPath: String): Result<Unit> = Result.success(Unit)

        override suspend fun loadModelByName(modelName: String, directory: File): Result<Unit> = Result.success(Unit)

        override suspend fun getLoadedModelName(): String = ""

        override suspend fun setLoadedModelName(modelName: String) {}

        override suspend fun getModelConfiguration(modelName: String): ModelConfiguration = ModelConfiguration()

        override suspend fun saveModelConfiguration(modelName: String, config: ModelConfiguration) {}

        override suspend fun modelExists(modelName: String, directory: File): Boolean = false

        override suspend fun getModelFileSize(modelName: String, directory: File): Long = 0L

        override suspend fun deleteModel(modelName: String, directory: File): Result<Unit> = Result.success(Unit)

        override suspend fun getDefaultModels(): List<Map<String, String>> {
            return defaultModelsResponse
        }
    }
}
