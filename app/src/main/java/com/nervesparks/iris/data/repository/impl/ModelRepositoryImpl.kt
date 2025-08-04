package com.nervesparks.iris.data.repository.impl

import android.llama.cpp.LLamaAndroid
import android.util.Log
import com.nervesparks.iris.data.repository.ModelRepository
import com.nervesparks.iris.data.repository.ModelConfiguration
import java.io.File

import javax.inject.Inject

/**
 * Implementation of ModelRepository for local model management
 */
class ModelRepositoryImpl @Inject constructor(
    private val llamaAndroid: LLamaAndroid
) : ModelRepository {
    
    private val tag = "ModelRepositoryImpl"
    private var currentLoadedModel: String = ""
    
    override suspend fun getAvailableModels(directory: File): List<Map<String, String>> {
        return try {
            // This would typically come from a configuration or database
            // For now, we'll return the hardcoded list and filter by existence
            val allModels = listOf(
                mapOf(
                    "name" to "Llama-3.2-3B-Instruct-Q4_K_L.gguf",
                    "source" to "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_L.gguf?download=true",
                    "destination" to "Llama-3.2-3B-Instruct-Q4_K_L.gguf"
                ),
                mapOf(
                    "name" to "Llama-3.2-1B-Instruct-Q6_K_L.gguf",
                    "source" to "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q6_K_L.gguf?download=true",
                    "destination" to "Llama-3.2-1B-Instruct-Q6_K_L.gguf"
                ),
                mapOf(
                    "name" to "stablelm-2-1_6b-chat.Q4_K_M.imx.gguf",
                    "source" to "https://huggingface.co/Crataco/stablelm-2-1_6b-chat-imatrix-GGUF/resolve/main/stablelm-2-1_6b-chat.Q4_K_M.imx.gguf?download=true",
                    "destination" to "stablelm-2-1_6b-chat.Q4_K_M.imx.gguf"
                ),
                mapOf(
                    "name" to "NemoTron-1.5B-Q4_K_M.gguf",
                    "source" to "https://huggingface.co/bartowski/nvidia_OpenReasoning-Nemotron-1.5B-GGUF/resolve/main/nvidia_OpenReasoning-Nemotron-1.5B-Q4_K_M.gguf?download=true",
                    "destination" to "NemoTron-1.5B-Q4_K_M.gguf"
                ),
                mapOf(
                    "name" to "Qwen_Qwen3-0.6B-Q4_K_M.gguf",
                    "source" to "https://huggingface.co/bartowski/Qwen_Qwen3-0.6B-GGUF/resolve/main/Qwen_Qwen3-0.6B-Q4_K_M.gguf?download=true",
                    "destination" to "Qwen_Qwen3-0.6B-Q4_K_M.gguf"
                )
            )
            
            // Filter to only include models that exist on disk
            allModels.filter { model ->
                val destinationPath = File(directory, model["destination"].toString())
                destinationPath.exists()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error getting available models", e)
            emptyList()
        }
    }
    
    override suspend fun loadModel(modelPath: String): Result<Unit> {
        return try {
            Log.d(tag, "Loading model: $modelPath")
            llamaAndroid.load(
                modelPath,
                userThreads = 2, // Default thread count
                topK = 40, // Default top-k
                topP = 0.9f, // Default top-p
                temp = 0.7f // Default temperature
            )
            currentLoadedModel = File(modelPath).name
            Log.i(tag, "Successfully loaded model: $currentLoadedModel")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error loading model: $modelPath", e)
            Result.failure(e)
        }
    }
    
    override suspend fun loadModelByName(modelName: String, directory: File): Result<Unit> {
        return try {
            val modelFile = File(directory, modelName)
            if (modelFile.exists()) {
                loadModel(modelFile.absolutePath)
            } else {
                Log.e(tag, "Model file not found: ${modelFile.absolutePath}")
                Result.failure(Exception("Model file not found: $modelName"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error loading model by name: $modelName", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getLoadedModelName(): String {
        return currentLoadedModel
    }
    
    override suspend fun setLoadedModelName(modelName: String) {
        currentLoadedModel = modelName
    }
    
    override suspend fun getModelConfiguration(modelName: String): ModelConfiguration {
        // TODO: Implement actual configuration loading from database
        return ModelConfiguration()
    }
    
    override suspend fun saveModelConfiguration(modelName: String, config: ModelConfiguration) {
        // TODO: Implement actual configuration saving to database
        Log.d(tag, "Saving configuration for model: $modelName")
    }
    
    override suspend fun modelExists(modelName: String, directory: File): Boolean {
        return try {
            val modelFile = File(directory, modelName)
            modelFile.exists()
        } catch (e: Exception) {
            Log.e(tag, "Error checking if model exists: $modelName", e)
            false
        }
    }
    
    override suspend fun getModelFileSize(modelName: String, directory: File): Long {
        return try {
            val modelFile = File(directory, modelName)
            if (modelFile.exists()) {
                modelFile.length()
            } else {
                0L
            }
        } catch (e: Exception) {
            Log.e(tag, "Error getting model file size: $modelName", e)
            0L
        }
    }
    
    override suspend fun deleteModel(modelName: String, directory: File): Result<Unit> {
        return try {
            val modelFile = File(directory, modelName)
            if (modelFile.exists()) {
                val deleted = modelFile.delete()
                if (deleted) {
                    Log.i(tag, "Successfully deleted model: $modelName")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to delete model file"))
                }
            } else {
                Result.failure(Exception("Model file not found"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error deleting model: $modelName", e)
            Result.failure(e)
        }
    }
} 