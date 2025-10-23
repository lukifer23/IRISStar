package com.nervesparks.iris.llm

import android.llama.cpp.LLamaAndroid
import com.nervesparks.iris.llm.ModelPerformanceTracker
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelLoaderTest {

    private lateinit var mockLlamaAndroid: LLamaAndroid
    private lateinit var mockPerformanceTracker: ModelPerformanceTracker
    private lateinit var modelLoader: ModelLoader

    @Before
    fun setUp() {
        mockLlamaAndroid = mockk(relaxed = true)
        mockPerformanceTracker = mockk(relaxed = true)
        modelLoader = ModelLoader(mockLlamaAndroid, mockPerformanceTracker)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `loadModel should return success when llamaAndroid load succeeds`() = runTest {
        // Given
        val modelPath = "/path/to/model.bin"
        val threadCount = 4
        val backend = "cpu"
        val temperature = 0.7f
        val topP = 0.9f
        val topK = 40
        val gpuLayers = -1

        // When
        val result = modelLoader.loadModel(
            modelPath = modelPath,
            threadCount = threadCount,
            backend = backend,
            temperature = temperature,
            topP = topP,
            topK = topK,
            gpuLayers = gpuLayers
        )

        // Then
        assertTrue(result.isSuccess)
        verify {
            mockLlamaAndroid.load(
                pathToModel = modelPath,
                userThreads = threadCount,
                topK = topK,
                topP = topP,
                temp = temperature,
                gpuLayers = gpuLayers
            )
        }
    }

    @Test
    fun `loadModel should return failure when llamaAndroid load fails`() = runTest {
        // Given
        val modelPath = "/path/to/model.bin"
        val exception = RuntimeException("Load failed")

        every { mockLlamaAndroid.load(any(), any(), any(), any(), any(), any()) } throws exception

        // When
        val result = modelLoader.loadModel(modelPath = modelPath)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `loadModelByName should return success when model file exists and loads successfully`() = runTest {
        // Given
        val modelName = "test-model.bin"
        val directory = File("/test/directory")
        val modelFile = File(directory, modelName)
        val modelPath = modelFile.absolutePath
        val sessionId = "session-123"

        // Mock directory listing
        mockkStatic(File::class)
        every { directory.listFiles() } returns arrayOf(modelFile)
        every { modelFile.exists() } returns true
        every { modelFile.absolutePath } returns modelPath

        // Mock successful load
        every { mockLlamaAndroid.load(any(), any(), any(), any(), any(), any()) } returns Unit
        every {
            mockPerformanceTracker.startSession(
                modelName = any(),
                modelPath = any(),
                configuration = any(),
                deviceInfo = any(),
                backendUsed = any()
            )
        } returns sessionId

        // When
        val result = modelLoader.loadModelByName(modelName = modelName, directory = directory)

        // Then
        assertTrue(result.isSuccess)
        val loadResult = result.getOrNull()
        assertEquals(modelPath, loadResult?.modelPath)
        assertEquals(sessionId, loadResult?.sessionId)
    }

    @Test
    fun `loadModelByName should return failure when model file does not exist`() = runTest {
        // Given
        val modelName = "nonexistent-model.bin"
        val directory = File("/test/directory")

        mockkStatic(File::class)
        every { directory.listFiles() } returns arrayOf()

        // When
        val result = modelLoader.loadModelByName(modelName = modelName, directory = directory)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Model not found: $modelName", result.exceptionOrNull()?.message)
    }

    @Test
    fun `unloadModel should return success when llamaAndroid unload succeeds`() = runTest {
        // Given
        every { mockLlamaAndroid.unload() } returns Unit

        // When
        val result = modelLoader.unloadModel()

        // Then
        assertTrue(result.isSuccess)
        verify { mockLlamaAndroid.unload() }
    }

    @Test
    fun `unloadModel should return failure when llamaAndroid unload fails`() = runTest {
        // Given
        val exception = RuntimeException("Unload failed")
        every { mockLlamaAndroid.unload() } throws exception

        // When
        val result = modelLoader.unloadModel()

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }


    @Test
    fun `loadModel should use default parameters when not specified`() = runTest {
        // Given
        val modelPath = "/path/to/model.bin"

        // When
        val result = modelLoader.loadModel(modelPath = modelPath)

        // Then
        assertTrue(result.isSuccess)
        verify {
            mockLlamaAndroid.load(
                pathToModel = modelPath,
                userThreads = 4,  // default
                topK = 40,     // default
                topP = 0.9f,   // default
                temp = 0.7f, // default
                gpuLayers = -1     // default
            )
        }
    }
}
