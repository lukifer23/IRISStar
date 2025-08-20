package com.nervesparks.iris.data.search

/**
 * Represents a single search result item.
 */
data class SearchResult(
    val title: String,
    val snippet: String,
    val url: String,
    val source: String
)

/**
 * Wrapper for search responses from various services.
 */
data class SearchResponse(
    val success: Boolean,
    val results: List<SearchResult>? = null,
    val error: String? = null,
    val query: String = ""
)
