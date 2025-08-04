package com.nervesparks.iris.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface HuggingFaceApiService {

    @GET("models")
    suspend fun searchModels(
        @Query("search") query: String,
        @Header("Authorization") token: String? = null
    ): List<ModelInfo>

    @GET("models/{modelId}")
    suspend fun getModelDetails(
        @Path("modelId") modelId: String,
        @Header("Authorization") token: String? = null
    ): ModelInfo
}

@JsonClass(generateAdapter = true)
data class ModelInfo(
    val id: String,
    @Json(name = "modelId") val name: String,
    val downloads: Int,
    val likes: Int,
    val tags: List<String>,
    val siblings: List<ModelFile> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ModelFile(
    @Json(name = "rfilename") val filename: String,
    val size: Long
)
