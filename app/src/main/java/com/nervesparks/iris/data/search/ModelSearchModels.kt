package com.nervesparks.iris.data.search

import com.nervesparks.iris.data.ModelFile

/**
 * Represents a single model result returned from Hugging Face search.
 */
data class ModelSearchResult(
    val id: String,
    val name: String,
    val description: String?,
    val downloads: Int,
    val likes: Int,
    val tags: List<String>
)

/**
 * Represents detailed information for a specific model, including its files.
 */
data class ModelDetailResult(
    val id: String,
    val name: String,
    val description: String?,
    val downloads: Int,
    val likes: Int,
    val tags: List<String>,
    val siblings: List<ModelFile>
)

typealias ModelSearchResponse = SearchResponse<ModelSearchResult>
typealias ModelDetailsResponse = SearchResponse<ModelDetailResult>
