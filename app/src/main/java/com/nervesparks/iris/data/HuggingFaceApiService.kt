package com.nervesparks.iris.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class HuggingFaceApiService(private val preferencesRepository: UserPreferencesRepository) {
    companion object {
        private const val TAG = "HuggingFaceApiService"
        private const val TIMEOUT = 30000
        private const val BASE_URL = "https://huggingface.co/api"
    }

    suspend fun searchModels(query: String): ApiResponse = withContext(Dispatchers.IO) {
        Log.d(TAG, "=== SEARCH MODELS START ===")
        Log.d(TAG, "Query: '$query'")
        
        val url = "$BASE_URL/models?search=$query&limit=10"
        Log.d(TAG, "Full URL: $url")
        
        return@withContext try {
            val connection = setupConnection(url)
            Log.d(TAG, "Connection established, response code: ${connection.responseCode}")
            
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    Log.d(TAG, "HTTP 200 OK - Reading response...")
                    val response = readResponse(connection)
                    Log.d(TAG, "Response length: ${response.length}")
                    Log.d(TAG, "Response preview: ${response.take(200)}...")
                    
                    val models = parseSearchResponse(response)
                    Log.d(TAG, "Parsed ${models.size} models")
                    ApiResponse(success = true, data = models)
                }
                HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    Log.e(TAG, "HTTP 401 Unauthorized - Check token")
                    ApiResponse(success = false, error = "Unauthorized - Check your HuggingFace token")
                }
                429 -> {
                    Log.e(TAG, "HTTP 429 Too Many Requests")
                    ApiResponse(success = false, error = "Too many requests - Please wait and try again")
                }
                HttpURLConnection.HTTP_NOT_FOUND -> {
                    Log.e(TAG, "HTTP 404 Not Found")
                    ApiResponse(success = false, error = "No models found for query: $query")
                }
                else -> {
                    Log.e(TAG, "HTTP ${connection.responseCode} - Unexpected response")
                    val errorResponse = readErrorResponse(connection)
                    Log.e(TAG, "Error response: $errorResponse")
                    ApiResponse(success = false, error = "Network error: HTTP ${connection.responseCode}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during search: ${e.message}", e)
            ApiResponse(success = false, error = "Network error: ${e.message}")
        } finally {
            Log.d(TAG, "=== SEARCH MODELS END ===")
        }
    }

    suspend fun getModelDetails(modelId: String): ApiResponse = withContext(Dispatchers.IO) {
        Log.d(TAG, "=== GET MODEL DETAILS START ===")
        Log.d(TAG, "Model ID: '$modelId'")
        
        val url = "$BASE_URL/models/$modelId"
        Log.d(TAG, "Full URL: $url")
        
        return@withContext try {
            val connection = setupConnection(url)
            Log.d(TAG, "Connection established, response code: ${connection.responseCode}")
            
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    Log.d(TAG, "HTTP 200 OK - Reading response...")
                    val response = readResponse(connection)
                    Log.d(TAG, "Response length: ${response.length}")
                    Log.d(TAG, "Response preview: ${response.take(200)}...")
                    
                    val model = parseDetailedModelFromJson(JSONObject(response))
                    Log.d(TAG, "Parsed model: ${model.name} with ${model.siblings.size} files")
                    ApiResponse(success = true, data = listOf(model))
                }
                HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    Log.e(TAG, "HTTP 401 Unauthorized - Check token")
                    ApiResponse(success = false, error = "Unauthorized - Check your HuggingFace token")
                }
                429 -> {
                    Log.e(TAG, "HTTP 429 Too Many Requests")
                    ApiResponse(success = false, error = "Too many requests - Please wait and try again")
                }
                HttpURLConnection.HTTP_NOT_FOUND -> {
                    Log.e(TAG, "HTTP 404 Not Found")
                    ApiResponse(success = false, error = "Model not found: $modelId")
                }
                else -> {
                    Log.e(TAG, "HTTP ${connection.responseCode} - Unexpected response")
                    val errorResponse = readErrorResponse(connection)
                    Log.e(TAG, "Error response: $errorResponse")
                    ApiResponse(success = false, error = "Network error: HTTP ${connection.responseCode}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during getModelDetails: ${e.message}", e)
            ApiResponse(success = false, error = "Network error: ${e.message}")
        } finally {
            Log.d(TAG, "=== GET MODEL DETAILS END ===")
        }
    }

    private fun setupConnection(url: String): HttpURLConnection {
        Log.d(TAG, "Setting up connection to: $url")
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "IRIS-Android-App/1.0")
        connection.connectTimeout = TIMEOUT
        connection.readTimeout = TIMEOUT
        connection.useCaches = false
        connection.doInput = true

        val token = preferencesRepository.getHuggingFaceToken()
        if (token.isNotEmpty()) {
            Log.d(TAG, "Using token: ${token.take(10)}...")
            connection.setRequestProperty("Authorization", "Bearer $token")
        } else {
            Log.d(TAG, "No token provided, using anonymous access")
        }
        
        Log.d(TAG, "Connection configured, attempting to connect...")
        return connection
    }

    private fun readResponse(connection: HttpURLConnection): String {
        Log.d(TAG, "Reading response from connection...")
        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()
        Log.d(TAG, "Response read successfully")
        return response.toString()
    }

    private fun readErrorResponse(connection: HttpURLConnection): String {
        Log.d(TAG, "Reading error response from connection...")
        return try {
            val reader = BufferedReader(InputStreamReader(connection.errorStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()
            response.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading error response: ${e.message}")
            "Unable to read error response"
        }
    }

    private fun parseSearchResponse(response: String): List<ModelInfo> {
        Log.d(TAG, "Parsing search response...")
        val models = mutableListOf<ModelInfo>()
        
        try {
            val jsonArray = JSONArray(response)
            Log.d(TAG, "Found ${jsonArray.length()} models in search results")
            
            for (i in 0 until jsonArray.length()) {
                try {
                    val modelJson = jsonArray.getJSONObject(i)
                    val model = parseModelFromJson(modelJson)
                    models.add(model)
                    Log.d(TAG, "Parsed model: ${model.name}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing model at index $i: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing search response: ${e.message}", e)
        }
        
        Log.d(TAG, "Successfully parsed ${models.size} models")
        return models
    }

    private fun parseModelFromJson(modelJson: JSONObject): ModelInfo {
        Log.d(TAG, "Parsing model from JSON: ${modelJson.optString("id", "unknown")}")
        
        val tags = mutableListOf<String>()
        val tagsArray = modelJson.optJSONArray("tags")
        tagsArray?.let {
            for (i in 0 until it.length()) {
                tags.add(it.getString(i))
            }
        }
        
        val modelId = modelJson.optString("id", "")
        val modelName = modelJson.optString("name", modelId)
        val description = modelJson.optString("description", "").takeIf { it.isNotEmpty() }
        
        Log.d(TAG, "Model parsed - ID: $modelId, Name: $modelName, Tags: ${tags.size}")
        
        return ModelInfo(
            id = modelId,
            name = modelName,
            description = description,
            downloads = modelJson.optInt("downloads", 0),
            likes = modelJson.optInt("likes", 0),
            tags = tags,
            siblings = emptyList() // Search results don't include siblings
        )
    }

    private fun parseDetailedModelFromJson(modelJson: JSONObject): ModelInfo {
        Log.d(TAG, "Parsing detailed model from JSON: ${modelJson.optString("id", "unknown")}")
        
        val modelId = modelJson.optString("id", "")
        val modelName = modelJson.optString("name", modelId)
        val description = modelJson.optString("description", "").takeIf { it.isNotEmpty() }

        val tags = mutableListOf<String>()
        val tagsArray = modelJson.optJSONArray("tags")
        if (tagsArray != null) {
            for (i in 0 until tagsArray.length()) {
                tags.add(tagsArray.getString(i))
            }
        }

        val siblings = mutableListOf<ModelFile>()
        val siblingsArray = modelJson.optJSONArray("siblings")
        if (siblingsArray != null) {
            Log.d(TAG, "Found ${siblingsArray.length()} sibling files")
            for (i in 0 until siblingsArray.length()) {
                try {
                    val fileJson = siblingsArray.getJSONObject(i)
                    val filename = fileJson.optString("rfilename", "")
                    val size = fileJson.optLong("size", 0)
                    val quantType = extractQuantizationInfo(filename)
                    
                    if (filename.contains(".gguf")) {
                        val estimatedSize = if (size > 0) size else estimateFileSize(filename, quantType)
                        siblings.add(ModelFile(filename, estimatedSize, quantType))
                        val sizeDisplay = when {
                            estimatedSize != null -> "${estimatedSize / (1024 * 1024)}MB"
                            else -> "Size unknown"
                        }
                        Log.d(TAG, "Added GGUF file: $filename ($sizeDisplay${quantType?.let { " - $it" } ?: ""})")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing sibling file at index $i: ${e.message}")
                }
            }
        } else {
            Log.d(TAG, "No siblings array found in detailed model response")
        }
        
        Log.d(TAG, "Detailed model parsed - ID: $modelId, Name: $modelName, Files: ${siblings.size}")
        
        return ModelInfo(
            id = modelId,
            name = modelName,
            description = description,
            downloads = modelJson.optInt("downloads", 0),
            likes = modelJson.optInt("likes", 0),
            tags = tags,
            siblings = siblings
        )
    }

    private fun extractQuantizationInfo(filename: String): String? {
        val quantPatterns = listOf(
            "Q2_K", "Q3_K", "Q4_K", "Q5_K", "Q6_K", "Q8_0",
            "Q2_0", "Q3_0", "Q4_0", "Q5_0", "Q6_0", "Q8_0",
            "Q4_1", "Q5_1", "Q6_1", "Q8_1"
        )
        for (pattern in quantPatterns) {
            if (filename.contains(pattern)) {
                return pattern
            }
        }
        return null
    }

    private fun estimateFileSize(filename: String, quantType: String?): Long? {
        // Extract model size from filename (e.g., "Qwen3-1.7B" -> 1.7B)
        val sizeMatch = Regex("(\\d+(?:\\.\\d+)?)([KMGT]?B)").find(filename)
        if (sizeMatch != null) {
            val sizeValue = sizeMatch.groupValues[1].toDouble()
            val sizeUnit = sizeMatch.groupValues[2]
            
            val baseSizeInBytes = when (sizeUnit) {
                "B" -> (sizeValue * 1).toLong()
                "KB" -> (sizeValue * 1024).toLong()
                "MB" -> (sizeValue * 1024 * 1024).toLong()
                "GB" -> (sizeValue * 1024 * 1024 * 1024).toLong()
                else -> null
            }
            
            // Apply quantization compression factor
            val compressionFactor = when (quantType) {
                "Q4_K" -> 0.25
                "Q4_0" -> 0.25
                "Q4_1" -> 0.25
                "Q5_K" -> 0.31
                "Q5_0" -> 0.31
                "Q5_1" -> 0.31
                "Q6_K" -> 0.38
                "Q8_0" -> 0.5
                "BF16" -> 0.5
                "F16" -> 0.5
                else -> 0.5 // Default compression
            }
            
            return baseSizeInBytes?.let { it * compressionFactor.toLong() }
        }
        return null
    }

    data class ModelInfo(
        val id: String,
        val name: String,
        val description: String?,
        val downloads: Int,
        val likes: Int,
        val tags: List<String>,
        val siblings: List<ModelFile> = emptyList()
    )

    data class ModelFile(
        val filename: String,
        val size: Long?,
        val quantType: String? = null
    )

    data class ApiResponse(
        val success: Boolean,
        val data: List<ModelInfo>? = null,
        val error: String? = null
    )
} 