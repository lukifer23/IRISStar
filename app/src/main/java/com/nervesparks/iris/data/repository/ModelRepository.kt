package com.nervesparks.iris.data.repository

import java.io.File

/**
 * Repository interface for model management operations
 */
interface ModelRepository {
    
    /**
     * Get all available models from the specified directory
     */
    suspend fun getAvailableModels(directory: File): List<Map<String, String>>

    /**
     * Refresh the model catalogue from remote or local storage and cache the results
     */
    suspend fun refreshAvailableModels(): List<Map<String, String>>
    
    /**
     * Load a model by file path
     */
    suspend fun loadModel(modelPath: String): Result<Unit>
    
    /**
     * Load a model by name from the specified directory
     */
    suspend fun loadModelByName(modelName: String, directory: File): Result<Unit>
    
    /**
     * Get the currently loaded model name
     */
    suspend fun getLoadedModelName(): String
    
    /**
     * Set the currently loaded model name
     */
    suspend fun setLoadedModelName(modelName: String)
    
    /**
     * Get model configuration (temperature, top-p, top-k, etc.)
     */
    suspend fun getModelConfiguration(modelName: String): ModelConfiguration
    
    /**
     * Save model configuration
     */
    suspend fun saveModelConfiguration(modelName: String, config: ModelConfiguration)
    
    /**
     * Check if a model file exists
     */
    suspend fun modelExists(modelName: String, directory: File): Boolean
    
    /**
     * Get model file size
     */
    suspend fun getModelFileSize(modelName: String, directory: File): Long
    
    /**
     * Delete a model file
     */
    suspend fun deleteModel(modelName: String, directory: File): Result<Unit>
}

/**
 * Data class for model configuration
 */
data class ModelConfiguration(
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val threadCount: Int = 2,
    val contextLength: Int = 4096,
    val systemPrompt: String = "",
    val gpuLayers: Int = -1,
)