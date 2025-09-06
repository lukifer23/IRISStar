package com.nervesparks.iris.viewmodel

import android.llama.cpp.LLamaAndroid
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nervesparks.iris.Downloadable
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.data.repository.ModelRepository
import com.nervesparks.iris.security.InputValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * PHASE 1.2: ModelViewModel - Extracted from MainViewModel
 * Handles model loading, switching, backend management, and hardware detection
 */
@HiltViewModel
class ModelViewModel @Inject constructor(
    private val llamaAndroid: LLamaAndroid,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val modelRepository: ModelRepository
) : ViewModel() {

    private val tag = "ModelViewModel"

    // Model state
    var currentModelName by mutableStateOf("")
    var isModelLoaded by mutableStateOf(false)
    var modelLoadingProgress by mutableStateOf(0f)

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

    // Model loading functions
    fun load(pathToModel: String, userThreads: Int, backend: String = "cpu") {
        viewModelScope.launch {
            try {
                Log.d(tag, "Loading model: $pathToModel with backend: $backend")

                // Set backend before loading
                selectBackend(backend)

                // Load the model
                llamaAndroid.load(pathToModel, userThreads, modelTopK, modelTopP, modelTemperature, modelGpuLayers)

                currentModelName = File(pathToModel).name
                isModelLoaded = true
                modelLoadingProgress = 1f

                Log.d(tag, "Model loaded successfully: $currentModelName")
            } catch (e: Exception) {
                Log.e(tag, "Error loading model: $pathToModel", e)
                isModelLoaded = false
                backendError = "Failed to load model: ${e.message}"
            }
        }
    }

    fun loadModel(modelPath: String) {
        load(modelPath, modelThreadCount, currentBackend)
    }

    fun loadModelByName(modelName: String, directory: File): Boolean {
        val modelFile = directory.listFiles()?.find { it.name == modelName }
        return if (modelFile != null) {
            loadModel(modelFile.absolutePath)
            true
        } else {
            Log.e(tag, "Model not found: $modelName")
            false
        }
    }

    fun unloadModel() {
        viewModelScope.launch {
            try {
                llamaAndroid.unload()
                isModelLoaded = false
                currentModelName = ""
                Log.d(tag, "Model unloaded")
            } catch (e: Exception) {
                Log.e(tag, "Error unloading model", e)
            }
        }
    }

    fun switchModel(modelName: String, directory: File) {
        viewModelScope.launch {
            try {
                unloadModel()
                loadModelByName(modelName, directory)
                Log.d(tag, "Switched to model: $modelName")
            } catch (e: Exception) {
                Log.e(tag, "Error switching model", e)
                backendError = "Failed to switch model: ${e.message}"
            }
        }
    }

    // Backend management
    fun selectBackend(backend: String) {
        viewModelScope.launch {
            try {
                val success = llamaAndroid.setBackend(backend.lowercase())
                if (success) {
                    currentBackend = backend
                    backendError = null
                    Log.d(tag, "Backend changed to: $backend")
                } else {
                    backendError = "Failed to switch backend to $backend"
                    Log.e(tag, "Failed to set backend: $backend")
                }
            } catch (e: Exception) {
                Log.e(tag, "Exception when setting backend to $backend", e)
                backendError = "Failed to switch backend: ${e.message}"
            }
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
                Log.d(tag, "Hardware detection: Available backends: $availableBackends")
                Log.d(tag, "Hardware detection: Optimal backend: $optimalBackend")
                Log.d(tag, "Hardware detection: Is Adreno GPU: $isAdrenoGpu")
            } catch (e: Exception) {
                Log.e(tag, "Hardware detection failed", e)
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
            Log.d(tag, "Model settings updated and validated")
        } else {
            Log.w(tag, "Invalid model settings provided - validation failed")
        }
    }

    private fun loadModelSettings() {
        try {
            modelTemperature = userPreferencesRepository.modelTemperature
            modelTopP = userPreferencesRepository.modelTopP
            modelTopK = userPreferencesRepository.modelTopK
            modelMaxTokens = userPreferencesRepository.modelMaxTokens
            modelContextLength = userPreferencesRepository.modelContextLength
            modelSystemPrompt = userPreferencesRepository.modelSystemPrompt
            modelChatFormat = userPreferencesRepository.modelChatFormat
            modelThreadCount = userPreferencesRepository.modelThreadCount
            modelGpuLayers = userPreferencesRepository.modelGpuLayers
            Log.d(tag, "Model settings loaded")
        } catch (e: Exception) {
            Log.e(tag, "Error loading model settings", e)
        }
    }

    private fun saveModelSettings() {
        userPreferencesRepository.modelTemperature = modelTemperature
        userPreferencesRepository.modelTopP = modelTopP
        userPreferencesRepository.modelTopK = modelTopK
        userPreferencesRepository.modelMaxTokens = modelMaxTokens
        userPreferencesRepository.modelContextLength = modelContextLength
        userPreferencesRepository.modelSystemPrompt = modelSystemPrompt
        userPreferencesRepository.modelChatFormat = modelChatFormat
        userPreferencesRepository.modelThreadCount = modelThreadCount
        userPreferencesRepository.modelGpuLayers = modelGpuLayers
        Log.d(tag, "Model settings saved")
    }

    // Available models management
    fun loadExistingModels(directory: File) {
        viewModelScope.launch {
            try {
                availableModels = modelRepository.getAvailableModels(directory)
                Log.d(tag, "Loaded ${availableModels.size} existing models")
            } catch (e: Exception) {
                Log.e(tag, "Error loading existing models", e)
            }
        }
    }

    fun getAvailableModels(directory: File): List<Map<String, String>> {
        return try {
            // TODO: This should be called from a coroutine
            emptyList()
        } catch (e: Exception) {
            Log.e(tag, "Error getting available models", e)
            emptyList()
        }
    }
}
