package com.nervesparks.iris.viewmodel

import android.llama.cpp.LLamaAndroid
import timber.log.Timber
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nervesparks.iris.Downloadable
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.data.repository.ModelRepository
import com.nervesparks.iris.llm.ModelLoadResult
import com.nervesparks.iris.llm.ModelLoader
import com.nervesparks.iris.llm.ModelPerformanceTracker
import com.nervesparks.iris.llm.ModelComparison
import com.nervesparks.iris.security.InputValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * PHASE 1.2: ModelViewModel - Extracted from MainViewModel
 * Handles model loading, switching, backend management, and hardware detection
 */
@HiltViewModel
class ModelViewModel @Inject constructor(
    private val llamaAndroid: LLamaAndroid,
    private val modelLoader: ModelLoader,
    private val performanceTracker: ModelPerformanceTracker,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val modelRepository: ModelRepository
) : ViewModel() {

    private val tag = "ModelViewModel"

    // Model state
    var currentModelName by mutableStateOf("")
    var isModelLoaded by mutableStateOf(false)
    var modelLoadingProgress by mutableStateOf(0f)
    var currentSessionId by mutableStateOf<String?>(null)

    // Backend and hardware
    var currentBackend by mutableStateOf("cpu")
    var availableBackends by mutableStateOf("")
    var isAdrenoGpu by mutableStateOf(false)
    var optimalBackend by mutableStateOf("cpu")
    var backendError by mutableStateOf<String?>(null)

    // Model settings
    var modelTemperature by mutableStateOf(0.7f)
    var modelTopP by mutableStateOf(0.9f)
    var modelTopK by mutableStateOf(40)
    var modelMaxTokens by mutableStateOf(2048)
    var modelContextLength by mutableStateOf(32768)
    var modelSystemPrompt by mutableStateOf("You are a helpful AI assistant.")
    var modelChatFormat by mutableStateOf("CHATML")
    var modelThreadCount by mutableStateOf(4)
    var modelGpuLayers by mutableStateOf(-1)

    // Available models
    var availableModels by mutableStateOf<List<Map<String, String>>>(emptyList())
    var downloadableModels by mutableStateOf<List<Downloadable>>(emptyList())

    init {
        loadModelSettings()
        detectHardwareCapabilities()
    }

    // Benchmark function (delegate to benchmark logic)
    fun runBenchmarkWithModel(modelName: String, directory: File) {
        // TODO: This should probably be in BenchmarkViewModel
        // For now, just log that benchmark was requested
        Timber.tag(tag).d("Benchmark requested for model: $modelName in directory: $directory")
        // The actual benchmark logic should be handled by the benchmark screen
    }

    // Model loading functions
    fun load(pathToModel: String, userThreads: Int, backend: String = "cpu") {
        viewModelScope.launch {
            try {
                Timber.tag(tag).d("Loading model: $pathToModel with backend: $backend")

                // Set loading state
                modelLoadingProgress = 0f
                backendError = null

                // Set backend before loading
                val backendSet = setBackend(backend)
                if (!backendSet) {
                    Timber.tag(tag).e("Failed to set backend to $backend. Aborting model load.")
                    isModelLoaded = false
                    modelLoadingProgress = 0f
                    currentSessionId = null
                    return@launch
                }

                // Use centralized model loader
                val result = modelLoader.loadModel(
                    modelPath = pathToModel,
                    threadCount = userThreads,
                    backend = backend,
                    temperature = modelTemperature,
                    topP = modelTopP,
                    topK = modelTopK,
                    gpuLayers = modelGpuLayers
                )

                result.fold(
                    onSuccess = { sessionId ->
                        currentModelName = File(pathToModel).name
                        isModelLoaded = true
                        modelLoadingProgress = 1f
                        currentSessionId = sessionId
                        backendError = null
                        Timber.tag(tag).d("Model loaded successfully: $currentModelName with session: $sessionId")
                    },
                    onFailure = { e ->
                        Timber.tag(tag).e(e, "Error loading model: $pathToModel")
                        isModelLoaded = false
                        modelLoadingProgress = 0f
                        currentSessionId = null
                        backendError = "Failed to load model: ${e.message}"
                    }
                )
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error in load function")
                isModelLoaded = false
                modelLoadingProgress = 0f
                backendError = "Failed to load model: ${e.message}"
            }
        }
    }

    fun loadModel(modelPath: String) {
        load(modelPath, modelThreadCount, currentBackend)
    }

    suspend fun loadModelByName(modelName: String, directory: File): Result<ModelLoadResult> {
        return try {
            Timber.tag(tag).d("Loading model by name: $modelName")

            // Set loading state
            modelLoadingProgress = 0f
            backendError = null

            val result = modelLoader.loadModelByName(
                modelName = modelName,
                directory = directory,
                threadCount = modelThreadCount,
                backend = currentBackend,
                temperature = modelTemperature,
                topP = modelTopP,
                topK = modelTopK,
                gpuLayers = modelGpuLayers
            )

            result.fold(
                onSuccess = { loadResult ->
                    currentModelName = File(loadResult.modelPath).name
                    isModelLoaded = true
                    modelLoadingProgress = 1f
                    currentSessionId = loadResult.sessionId
                    backendError = null
                    Timber.tag(tag).d("Model loaded successfully: $currentModelName with session: ${loadResult.sessionId}")
                    Result.success(loadResult)
                },
                onFailure = { e ->
                    Timber.tag(tag).e(e, "Error loading model by name: $modelName")
                    isModelLoaded = false
                    modelLoadingProgress = 0f
                    currentSessionId = null
                    backendError = "Failed to load model: ${e.message}"
                    Result.failure(e)
                }
            )
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Exception loading model by name: $modelName")
            isModelLoaded = false
            modelLoadingProgress = 0f
            currentSessionId = null
            backendError = "Failed to load model: ${e.message}"
            Result.failure(e)
        }
    }

    fun unloadModel() {
        viewModelScope.launch {
            try {
                // End performance tracking session if active
                currentSessionId?.let { sessionId ->
                    modelLoader.endSession(sessionId)
                }

                val result = modelLoader.unloadModel()
                result.fold(
                    onSuccess = {
                        Timber.tag(tag).d("Model unloaded successfully")
                        isModelLoaded = false
                        currentModelName = ""
                        currentSessionId = null
                        backendError = null

                        // Help with memory cleanup
                        System.gc()
                    },
                    onFailure = { e ->
                        Timber.tag(tag).e(e, "Error unloading model")
                        backendError = "Failed to unload model: ${e.message}"
                    }
                )
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error in unloadModel function")
            }
        }
    }

    /**
     * Force cleanup of resources to help with memory management
     */
    fun cleanupResources() {
        viewModelScope.launch {
            try {
                // Clear cached lists to free memory
                availableModels = emptyList()
                downloadableModels = emptyList()

                // Clear any error states
                backendError = null

                // Force garbage collection
                System.gc()
                System.runFinalization()

                Timber.tag(tag).d("Resources cleaned up")
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error during resource cleanup")
            }
        }
    }

    // Performance tracking methods
    fun getPerformanceComparison(): List<ModelComparison> {
        return modelLoader.getPerformanceComparison()
    }

    fun getBestPerformingModel(): ModelPerformanceTracker.ModelMetrics? {
        return modelLoader.getBestPerformingModel()
    }

    fun clearPerformanceData() {
        performanceTracker.clearAllData()
    }

    fun recordInferencePerformance(tokensGenerated: Int, inferenceTime: Long, memoryUsage: Long) {
        currentSessionId?.let { sessionId ->
            modelLoader.recordInference(
                sessionId = sessionId,
                tokensGenerated = tokensGenerated,
                inferenceTime = inferenceTime,
                memoryUsage = memoryUsage
            )
        }
    }

    fun switchModel(modelName: String, directory: File) {
        viewModelScope.launch {
            try {
                unloadModel()
                loadModelByName(modelName, directory)
                Timber.tag(tag).d("Switched to model: $modelName")
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error switching model")
                backendError = "Failed to switch model: ${e.message}"
            }
        }
    }

    // Backend management
    fun selectBackend(backend: String) {
        viewModelScope.launch {
            setBackend(backend)
        }
    }

    private suspend fun setBackend(backend: String): Boolean {
        return try {
            val success = withContext(Dispatchers.IO) {
                llamaAndroid.setBackend(backend.lowercase())
            }
            if (success) {
                currentBackend = backend
                backendError = null
                Timber.tag(tag).d("Backend changed to: $backend")
            } else {
                backendError = "Failed to switch backend to $backend"
                Timber.tag(tag).e("Failed to set backend: $backend")
            }
            success
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Exception when setting backend to $backend")
            backendError = "Failed to switch backend: ${e.message}"
            false
        }
    }

    fun detectHardwareCapabilities() {
        viewModelScope.launch {
            try {
                val backends = llamaAndroid.getAvailableBackends().split(",").map { it.trim() }
                availableBackends = backends.joinToString(",")

                isAdrenoGpu = llamaAndroid.isAdrenoGpu()
                optimalBackend = if (isAdrenoGpu) "opencl" else "cpu"

                backendError = null
                Timber.tag(tag).d("Hardware detection: Available backends: $availableBackends")
                Timber.tag(tag).d("Hardware detection: Optimal backend: $optimalBackend")
                Timber.tag(tag).d("Hardware detection: Is Adreno GPU: $isAdrenoGpu")
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Hardware detection failed")
                isAdrenoGpu = false
                backendError = "Hardware detection failed: ${e.message}"
            }
        }
    }

    // Model settings
    fun updateModelSettings(
        temperature: Float,
        topP: Float,
        topK: Int,
        maxTokens: Int,
        contextLength: Int,
        systemPrompt: String,
        chatFormat: String,
        threadCount: Int,
        gpuLayers: Int
    ) {
        // Validate all inputs
        val isValidTemperature = InputValidator.isValidTemperature(temperature)
        val isValidTopP = InputValidator.isValidTopP(topP)
        val isValidTopK = InputValidator.isValidTopK(topK)
        val isValidMaxTokens = InputValidator.isValidMaxTokens(maxTokens)
        val isValidContextLength = InputValidator.isValidContextLength(contextLength)
        val sanitizedPrompt = InputValidator.sanitizeTextInput(systemPrompt)

        if (isValidTemperature && isValidTopP && isValidTopK &&
            isValidMaxTokens && isValidContextLength) {

            modelTemperature = temperature
            modelTopP = topP
            modelTopK = topK
            modelMaxTokens = maxTokens
            modelContextLength = contextLength
            modelSystemPrompt = sanitizedPrompt
            modelChatFormat = chatFormat
            modelThreadCount = threadCount
            modelGpuLayers = gpuLayers

            saveModelSettings()
            Timber.tag(tag).d("Model settings updated and validated")
        } else {
            Timber.tag(tag).w("Invalid model settings provided - validation failed")
        }
    }

    private fun loadModelSettings() {
        viewModelScope.launch {
            try {
                modelTemperature = userPreferencesRepository.getModelTemperature()
                modelTopP = userPreferencesRepository.getModelTopP()
                modelTopK = userPreferencesRepository.getModelTopK()
                modelMaxTokens = userPreferencesRepository.getModelMaxTokens()
                modelContextLength = userPreferencesRepository.getModelContextLength()
                modelSystemPrompt = userPreferencesRepository.getModelSystemPrompt()
                modelChatFormat = userPreferencesRepository.getModelChatFormat()
                modelThreadCount = userPreferencesRepository.getModelThreadCount()
                modelGpuLayers = userPreferencesRepository.getModelGpuLayers()
                Timber.tag(tag).d("Model settings loaded")
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error loading model settings")
            }
        }
    }

    private fun saveModelSettings() {
        viewModelScope.launch {
            try {
                userPreferencesRepository.setModelTemperature(modelTemperature)
                userPreferencesRepository.setModelTopP(modelTopP)
                userPreferencesRepository.setModelTopK(modelTopK)
                userPreferencesRepository.setModelMaxTokens(modelMaxTokens)
                userPreferencesRepository.setModelContextLength(modelContextLength)
                userPreferencesRepository.setModelSystemPrompt(modelSystemPrompt)
                userPreferencesRepository.setModelChatFormat(modelChatFormat)
                userPreferencesRepository.setModelThreadCount(modelThreadCount)
                userPreferencesRepository.setModelGpuLayers(modelGpuLayers)
                Timber.tag(tag).d("Model settings saved")
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error saving model settings")
            }
        }
    }

    // Available models management
    fun loadExistingModels(directory: File) {
        viewModelScope.launch {
            try {
                availableModels = modelRepository.getAvailableModels(directory)
                Timber.tag(tag).d("Loaded ${availableModels.size} existing models")
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error loading existing models")
            }
        }
    }

    fun getAvailableModels(directory: File): List<Map<String, String>> {
        return try {
            // TODO: This should be called from a coroutine
            emptyList()
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error getting available models")
            emptyList()
        }
    }
}
