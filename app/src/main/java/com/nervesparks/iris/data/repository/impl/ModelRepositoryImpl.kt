package com.nervesparks.iris.data.repository.impl

import android.content.Context
import android.llama.cpp.LLamaAndroid
import timber.log.Timber
import com.nervesparks.iris.data.HuggingFaceApiService
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.data.exceptions.ModelNotFoundException
import com.nervesparks.iris.data.exceptions.InvalidModelException
import com.nervesparks.iris.data.exceptions.ValidationException
import com.nervesparks.iris.data.exceptions.NetworkException
import com.nervesparks.iris.data.repository.ModelRepository
import com.nervesparks.iris.data.repository.ModelConfiguration
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File

import javax.inject.Inject

/**
 * Implementation of ModelRepository for local model management
 */
class ModelRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val llamaAndroid: LLamaAndroid,
    private val huggingFaceApiService: HuggingFaceApiService,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val moshi: Moshi
) : ModelRepository {
    
    private val tag = "ModelRepositoryImpl"
    private var currentLoadedModel: String = ""
    private var cachedModels: List<Map<String, String>>? = null
    private var defaultModels: List<Map<String, String>>? = null

    private data class DefaultModel(
        val name: String,
        val source: String,
        val destination: String,
        val supportsReasoning: Boolean = false,
        val supportsVision: Boolean = false,
        val chatTemplate: String? = null
    )

    private fun curatedDefaultModels(): List<Map<String, String>> {
        if (defaultModels == null) {
            defaultModels = try {
                val json = context.assets.open("default_models.json").bufferedReader().use { it.readText() }
                val type = Types.newParameterizedType(List::class.java, DefaultModel::class.java)
                val adapter = moshi.adapter<List<DefaultModel>>(type)
                adapter.fromJson(json)?.map { model ->
                    mutableMapOf(
                        "name" to model.name,
                        "source" to model.source,
                        "destination" to model.destination,
                        "supportsReasoning" to model.supportsReasoning.toString()
                    ).apply {
                        if (model.supportsVision) {
                            put("supportsVision", model.supportsVision.toString())
                        }
                        model.chatTemplate?.let { put("chatTemplate", it) }
                    }
                }
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error loading default models config")
                emptyList()
            }
        }
        return defaultModels ?: emptyList()
    }

    private data class CachedModel(
        val name: String,
        val source: String,
        val destination: String,
        val supportsReasoning: Boolean = false
    )

    override suspend fun refreshAvailableModels(): List<Map<String, String>> {
        return try {
            // Validate input parameters
            val searchQuery = "gguf"
            if (searchQuery.isBlank()) {
                throw ValidationException("Search query cannot be blank")
            }
            
            val token = userPreferencesRepository.huggingFaceToken
                .takeIf { it.isNotEmpty() }
                ?.let { storedToken ->
                    if (storedToken.startsWith("Bearer ")) storedToken else "Bearer $storedToken"
                }
            val models = huggingFaceApiService.searchModels(searchQuery, token)
            val mapped = models.flatMap { info ->
                info.siblings.filter { it.filename.endsWith(".gguf") }.map { file ->
                    val source = "https://huggingface.co/${info.id}/resolve/main/${file.filename}?download=true"
                    mapOf(
                        "name" to file.filename,
                        "source" to source,
                        "destination" to file.filename,
                        "supportsReasoning" to "false"
                    )
                }
            }
            
            // Curated default models (centralized)
            val defaultModels = curatedDefaultModels()
            
            val allModels = defaultModels + mapped
            cachedModels = allModels

            // persist cache
            val listType = Types.newParameterizedType(List::class.java, CachedModel::class.java)
            val adapter = moshi.adapter<List<CachedModel>>(listType)
            val cacheData = allModels.map {
                CachedModel(
                    it["name"] ?: "",
                    it["source"] ?: "",
                    it["destination"] ?: "",
                    it["supportsReasoning"]?.toBoolean() ?: false
                )
            }
            userPreferencesRepository.setCachedModels(adapter.toJson(cacheData))

            allModels
        } catch (e: NetworkException) {
            Timber.tag(tag).e(e, "Network error refreshing model catalogue")
            throw e
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error refreshing model catalogue")
            // Return curated default models even if API fails
            curatedDefaultModels()
        }
    }

    override suspend fun getDefaultModels(): List<Map<String, String>> {
        return curatedDefaultModels()
    }

    override suspend fun getAvailableModels(directory: File): List<Map<String, String>> {
        return try {
            val installedModels = listInstalledModels(directory)
            if (installedModels.isEmpty()) {
                return emptyList()
            }

            val metadata = ensureCachedModels()
            val metadataByDestination = metadata.associateBy { entry ->
                entry["destination"].orEmpty().ifEmpty { entry["name"].orEmpty() }
            }

            installedModels.map { file ->
                val cached = metadataByDestination[file.name]
                    ?: metadataByDestination[file.nameWithoutExtension]
                buildLocalModelEntry(file, cached)
            }
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error getting available models")
            emptyList()
        }
    }

    private suspend fun ensureCachedModels(): List<Map<String, String>> {
        if (cachedModels == null) {
            val json = userPreferencesRepository.getCachedModels()
            if (json.isNotEmpty()) {
                runCatching {
                    val listType = Types.newParameterizedType(List::class.java, CachedModel::class.java)
                    val adapter = moshi.adapter<List<CachedModel>>(listType)
                    cachedModels = adapter.fromJson(json)?.map { model ->
                        mapOf(
                            "name" to model.name,
                            "source" to model.source,
                            "destination" to model.destination,
                            "supportsReasoning" to model.supportsReasoning.toString()
                        )
                    }
                }.onFailure { error ->
                    Timber.tag(tag).e(error, "Error parsing cached models JSON")
                    cachedModels = null
                }
            }
        }

        if (cachedModels == null) {
            cachedModels = refreshAvailableModels()
        }

        return cachedModels ?: emptyList()
    }

    private fun listInstalledModels(directory: File): List<File> {
        if (!directory.exists() || !directory.isDirectory) {
            return emptyList()
        }

        return directory.listFiles { file ->
            file.isFile && file.extension.equals("gguf", ignoreCase = true)
        }?.toList().orEmpty()
    }

    private fun buildLocalModelEntry(file: File, cached: Map<String, String>?): Map<String, String> {
        val base = mutableMapOf(
            "name" to file.name,
            "source" to file.toURI().toString(),
            "destination" to file.name,
            "supportsReasoning" to "false"
        )

        cached?.let { cachedEntry ->
            base.putAll(cachedEntry)
            base["destination"] = cachedEntry["destination"].orEmpty().ifEmpty { file.name }
        }

        return base
    }
    
    override suspend fun loadModel(modelPath: String): Result<Unit> {
        return try {
            // Validate input parameters
            if (modelPath.isBlank()) {
                throw ValidationException("Model path cannot be blank")
            }
            
            val modelFile = File(modelPath)
            if (!modelFile.exists()) {
                throw ModelNotFoundException(modelFile.name)
            }
            
            if (!modelFile.canRead()) {
                throw InvalidModelException(modelFile.name, Exception("File is not readable"))
            }

            // Retrieve model configuration with defaults fallback
            val config = runCatching { getModelConfiguration(modelFile.name) }
                .getOrElse { ModelConfiguration() }

            Timber.tag(tag).d("Loading model: $modelPath")
            llamaAndroid.load(
                modelPath,
                userThreads = config.threadCount,
                topK = config.topK,
                topP = config.topP,
                temp = config.temperature,
                gpuLayers = config.gpuLayers
            )
            currentLoadedModel = modelFile.name
            Timber.tag(tag).i("Successfully loaded model: $currentLoadedModel")
            Result.success(Unit)
        } catch (e: ModelNotFoundException) {
            Timber.tag(tag).e(e, "Model not found: $modelPath")
            Result.failure(e)
        } catch (e: InvalidModelException) {
            Timber.tag(tag).e(e, "Invalid model: $modelPath")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error loading model: $modelPath")
            Result.failure(e)
        }
    }
    
    override suspend fun loadModelByName(modelName: String, directory: File): Result<Unit> {
        return try {
            // Validate input parameters
            if (modelName.isBlank()) {
                throw IllegalArgumentException("Model name cannot be blank")
            }
            
            if (!directory.exists()) {
                throw IllegalArgumentException("Directory does not exist: ${directory.absolutePath}")
            }
            
            if (!directory.isDirectory) {
                throw IllegalArgumentException("Path is not a directory: ${directory.absolutePath}")
            }
            
            if (!directory.canRead()) {
                throw IllegalArgumentException("Directory is not readable: ${directory.absolutePath}")
            }
            
            val modelFile = File(directory, modelName)
            if (modelFile.exists()) {
                loadModel(modelFile.absolutePath)
            } else {
                Timber.tag(tag).e("Model file not found: ${modelFile.absolutePath}")
                Result.failure(Exception("Model file not found: $modelName"))
            }
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error loading model by name: $modelName")
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
            Timber.tag(tag).e(e, "Error getting model configuration for $modelName")
            ModelConfiguration()
        }
    }

    override suspend fun saveModelConfiguration(modelName: String, config: ModelConfiguration) {
        try {
            userPreferencesRepository.saveModelConfiguration(modelName, config)
            Timber.tag(tag).d("Saving configuration for model: $modelName")
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error saving model configuration for $modelName")
        }
    }
    
    override suspend fun modelExists(modelName: String, directory: File): Boolean {
        return try {
            val modelFile = File(directory, modelName)
            modelFile.exists()
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error checking if model exists: $modelName")
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
            Timber.tag(tag).e(e, "Error getting model file size: $modelName")
            0L
        }
    }
    
    override suspend fun deleteModel(modelName: String, directory: File): Result<Unit> {
        return try {
            val modelFile = File(directory, modelName)
            if (modelFile.exists()) {
                val deleted = modelFile.delete()
                if (deleted) {
                    Timber.tag(tag).i("Successfully deleted model: $modelName")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to delete model file"))
                }
            } else {
                Result.failure(Exception("Model file not found"))
            }
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error deleting model: $modelName")
            Result.failure(e)
        }
    }
} 
