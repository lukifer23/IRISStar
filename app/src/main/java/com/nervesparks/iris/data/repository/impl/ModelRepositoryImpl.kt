package com.nervesparks.iris.data.repository.impl

import android.llama.cpp.LLamaAndroid
import android.util.Log
import com.nervesparks.iris.data.HuggingFaceApiService
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.data.repository.ModelRepository
import com.nervesparks.iris.data.repository.ModelConfiguration
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.File

import javax.inject.Inject

/**
 * Implementation of ModelRepository for local model management
 */
class ModelRepositoryImpl @Inject constructor(
    private val llamaAndroid: LLamaAndroid,
    private val huggingFaceApiService: HuggingFaceApiService,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val moshi: Moshi
) : ModelRepository {
    
    private val tag = "ModelRepositoryImpl"
    private var currentLoadedModel: String = ""
    private var cachedModels: List<Map<String, String>>? = null

    private data class CachedModel(val name: String, val source: String, val destination: String)

    override suspend fun refreshAvailableModels(): List<Map<String, String>> {
        return try {
            val token = userPreferencesRepository.getHuggingFaceToken().takeIf { it.isNotEmpty() }
            val models = huggingFaceApiService.searchModels("gguf", token)
            val mapped = models.flatMap { info ->
                info.siblings.filter { it.filename.endsWith(".gguf") }.map { file ->
                    val source = "https://huggingface.co/${info.id}/resolve/main/${file.filename}?download=true"
                    mapOf(
                        "name" to file.filename,
                        "source" to source,
                        "destination" to file.filename
                    )
                }
            }
            cachedModels = mapped

            // persist cache
            val listType = Types.newParameterizedType(List::class.java, CachedModel::class.java)
            val adapter = moshi.adapter<List<CachedModel>>(listType)
            val cacheData = mapped.map { CachedModel(it["name"] ?: "", it["source"] ?: "", it["destination"] ?: "") }
            userPreferencesRepository.setCachedModels(adapter.toJson(cacheData))

            mapped
        } catch (e: Exception) {
            Log.e(tag, "Error refreshing model catalogue", e)
            emptyList()
        }
    }

    override suspend fun getAvailableModels(directory: File): List<Map<String, String>> {
        return try {
            if (cachedModels == null) {
                val json = userPreferencesRepository.getCachedModels()
                if (json.isNotEmpty()) {
                    val listType = Types.newParameterizedType(List::class.java, CachedModel::class.java)
                    val adapter = moshi.adapter<List<CachedModel>>(listType)
                    cachedModels = adapter.fromJson(json)?.map { model ->
                        mapOf(
                            "name" to model.name,
                            "source" to model.source,
                            "destination" to model.destination
                        )
                    }
                }
            }
            if (cachedModels == null) {
                refreshAvailableModels()
            }

            (cachedModels ?: emptyList()).filter { model ->
                val destinationPath = File(directory, model["destination"].orEmpty())
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
        return try {
            userPreferencesRepository.getModelConfiguration(modelName)
        } catch (e: Exception) {
            Log.e(tag, "Error getting model configuration for $modelName", e)
            ModelConfiguration()
        }
    }

    override suspend fun saveModelConfiguration(modelName: String, config: ModelConfiguration) {
        try {
            userPreferencesRepository.saveModelConfiguration(modelName, config)
            Log.d(tag, "Saving configuration for model: $modelName")
        } catch (e: Exception) {
            Log.e(tag, "Error saving model configuration for $modelName", e)
        }
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