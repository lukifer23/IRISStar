package com.nervesparks.iris.llm

import android.llama.cpp.LLamaAndroid
import timber.log.Timber
import com.nervesparks.iris.data.exceptions.ErrorHandler
import com.nervesparks.iris.llm.ModelPerformanceTracker
import java.io.File
import javax.inject.Inject

/**
 * Centralized model loading service to eliminate duplicate code between MainViewModel and ModelViewModel.
 *
 * This service provides a unified interface for all model loading operations, ensuring consistent
 * error handling, logging, and state management across the application.
 *
 * @param llamaAndroid The LLamaAndroid instance for native model operations
 */
class ModelLoader @Inject constructor(
    private val llamaAndroid: LLamaAndroid,
    private val performanceTracker: ModelPerformanceTracker
) {

    private val tag = "ModelLoader"

    /**
     * Load a model by file path with specified configuration and performance tracking
     */
    suspend fun loadModel(
        modelPath: String,
        threadCount: Int = 4,
        backend: String = "cpu",
        temperature: Float = 0.7f,
        topP: Float = 0.9f,
        topK: Int = 40,
        gpuLayers: Int = -1
    ): Result<String> {
        return try {
            Timber.tag(tag).d("Loading model: $modelPath with backend: $backend")

            val modelName = File(modelPath).name
            val startTime = System.currentTimeMillis()

            // Load the model with all parameters
            llamaAndroid.load(
                modelPath = modelPath,
                nThreads = threadCount,
                topK = topK,
                topP = topP,
                temperature = temperature,
                nGpuLayers = gpuLayers
            )

            val loadTime = System.currentTimeMillis() - startTime

            // Start performance tracking session
            val sessionId = performanceTracker.startSession(
                modelName = modelName,
                modelPath = modelPath,
                configuration = ModelPerformanceTracker.ModelConfiguration(
                    temperature = temperature,
                    topP = topP,
                    topK = topK,
                    threadCount = threadCount,
                    gpuLayers = gpuLayers,
                    contextLength = 2048, // Default context length
                    chatFormat = "CHATML"
                ),
                deviceInfo = ModelPerformanceTracker.DeviceInfo(
                    deviceModel = "Unknown",
                    androidVersion = "Unknown",
                    availableMemory = Runtime.getRuntime().maxMemory(),
                    cpuCores = Runtime.getRuntime().availableProcessors(),
                    hasGpu = false // Would need device capability detection
                ),
                backendUsed = backend
            )

            Timber.tag(tag).d("Model loaded successfully: $modelName in ${loadTime}ms")
            Result.success(sessionId)
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error loading model: $modelPath")
            ErrorHandler.reportModelError(e, File(modelPath).name)
            Result.failure(e)
        }
    }

    /**
     * Load a model by name from a directory
     */
    suspend fun loadModelByName(
        modelName: String,
        directory: File,
        threadCount: Int = 4,
        backend: String = "cpu",
        temperature: Float = 0.7f,
        topP: Float = 0.9f,
        topK: Int = 40,
        gpuLayers: Int = -1
    ): Result<String> {
        return try {
            Timber.tag(tag).d("Loading model by name: $modelName from directory: ${directory.absolutePath}")

            val modelFile = directory.listFiles()?.find { it.name == modelName }
                ?: return Result.failure(IllegalArgumentException("Model not found: $modelName"))

            // Load the model and get the session ID
            val loadResult = loadModel(
                modelPath = modelFile.absolutePath,
                threadCount = threadCount,
                backend = backend,
                temperature = temperature,
                topP = topP,
                topK = topK,
                gpuLayers = gpuLayers
            )

            loadResult.fold(
                onSuccess = { sessionId ->
                    Result.success(modelFile.absolutePath)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error loading model by name: $modelName")
            ErrorHandler.reportModelError(e, modelName)
            Result.failure(e)
        }
    }

    /**
     * Unload the current model
     */
    fun unloadModel(): Result<Unit> {
        return try {
            llamaAndroid.unload()
            Timber.tag(tag).d("Model unloaded successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error unloading model")
            ErrorHandler.reportError(e, "Model Unloading", ErrorHandler.ErrorSeverity.MEDIUM, "Failed to unload model")
            Result.failure(e)
        }
    }

    /**
     * Check if a model is currently loaded
     */
    fun isModelLoaded(): Boolean {
        return try {
            llamaAndroid.isModelLoaded()
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error checking if model is loaded")
            false
        }
    }

    /**
     * Record inference performance for the current session
     */
    fun recordInference(
        sessionId: String,
        tokensGenerated: Int,
        inferenceTime: Long,
        memoryUsage: Long
    ) {
        performanceTracker.recordInference(
            sessionId = sessionId,
            tokensGenerated = tokensGenerated,
            inferenceTime = inferenceTime,
            memoryUsage = memoryUsage
        )
    }

    /**
     * End the current performance tracking session
     */
    fun endSession(sessionId: String) {
        performanceTracker.endSession(sessionId)
    }

    /**
     * Get model performance comparison data
     */
    fun getPerformanceComparison(): List<ModelComparison> {
        return performanceTracker.getPerformanceComparison()
    }

    /**
     * Get the best performing model
     */
    fun getBestPerformingModel(): ModelPerformanceTracker.ModelMetrics? {
        return performanceTracker.getBestPerformingModel()
    }
}
