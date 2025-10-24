package com.nervesparks.iris.data.search

/**
 * Represents a single search result item.
 */
data class SearchResult(
    val title: String,
    val snippet: String,
    val url: String,
    val source: String,
    val confidence: Float? = null
)

/**
 * Wrapper for search responses from various services.
 */
data class SearchResponse<T>(
    val success: Boolean,
    val results: List<T>? = null,
    val error: String? = null,
    val query: String = ""
)
