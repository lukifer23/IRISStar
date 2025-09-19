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
                Timber.tag(tag).d("Loading model: $pathToModel with backend: $backend")

                // Set backend before loading
                selectBackend(backend)

                // Load the model
                llamaAndroid.load(pathToModel, userThreads, modelTopK, modelTopP, modelTemperature, modelGpuLayers)

                currentModelName = File(pathToModel).name
                isModelLoaded = true
                modelLoadingProgress = 1f

                Timber.tag(tag).d("Model loaded successfully: $currentModelName")
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error loading model: $pathToModel")
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
            Timber.tag(tag).e("Model not found: $modelName")
            false
        }
    }

    fun unloadModel() {
        viewModelScope.launch {
            try {
                llamaAndroid.unload()
                isModelLoaded = false
                currentModelName = ""
                Timber.tag(tag).d("Model unloaded")
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error unloading model")
            }
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
            try {
                val success = llamaAndroid.setBackend(backend.lowercase())
                if (success) {
                    currentBackend = backend
                    backendError = null
                    Timber.tag(tag).d("Backend changed to: $backend")
                } else {
                    backendError = "Failed to switch backend to $backend"
                    Timber.tag(tag).e("Failed to set backend: $backend")
                }
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Exception when setting backend to $backend")
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
