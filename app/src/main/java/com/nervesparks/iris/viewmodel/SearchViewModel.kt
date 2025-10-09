package com.nervesparks.iris.viewmodel

import timber.log.Timber
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nervesparks.iris.data.WebSearchService
import com.nervesparks.iris.data.AndroidSearchService
import com.nervesparks.iris.data.exceptions.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PHASE 1.3: SearchViewModel - Extracted from MainViewModel
 * Handles web search, document search, and related functionality
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val webSearchService: WebSearchService,
    private val androidSearchService: AndroidSearchService
) : ViewModel() {

    private val tag = "SearchViewModel"

    // Search state
    var isSearching by mutableStateOf(false)
    var searchResults by mutableStateOf<List<Map<String, String>>>(emptyList())
    var searchError by mutableStateOf<String?>(null)

    // Search caching for performance
    private val searchCache = mutableMapOf<String, SearchCacheEntry>()
    private val maxCacheSize = 50
    private val cacheExpiryMs = 30 * 60 * 1000L // 30 minutes

    /**
     * Cache entry for search results
     */
    data class SearchCacheEntry(
        val results: List<Map<String, String>>,
        val timestamp: Long,
        val query: String
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - timestamp > 30 * 60 * 1000L // 30 minutes
        }
    }

    // Web search function with caching
    fun performWebSearch(query: String, summarize: Boolean = true) {
        viewModelScope.launch {
            try {
                isSearching = true
                searchError = null

                // Check cache first
                val cacheKey = "web:$query"
                val cachedEntry = searchCache[cacheKey]

                if (cachedEntry != null && !cachedEntry.isExpired()) {
                    Timber.tag(tag).d("Using cached search results for: $query")
                    searchResults = cachedEntry.results
                    isSearching = false
                    return@launch
                }

                Timber.tag(tag).d("Performing web search for: $query")
                val response = webSearchService.searchWeb(query)

                if (response.success && response.results?.isNotEmpty() == true) {
                    searchResults = response.results.map { result ->
                        mapOf(
                            "title" to result.title,
                            "snippet" to result.snippet,
                            "link" to result.url
                        )
                    }

                    // Cache the results
                    cacheSearchResults(cacheKey, searchResults, query)

                    if (summarize && searchResults.isNotEmpty()) {
                        Timber.tag(tag).d("Search completed with ${searchResults.size} results - summarization available")
                    }
                } else {
                    searchResults = emptyList()
                    searchError = response.error ?: "No search results found"
                }

            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error performing web search")
                ErrorHandler.reportError(e, "Web Search", ErrorHandler.ErrorSeverity.MEDIUM, "Web search failed. Please check your internet connection and try again.")
                searchError = "Search failed: ${e.message}"
                searchResults = emptyList()
            } finally {
                isSearching = false
            }
        }
    }

    // Document search function with caching
    fun searchDocuments(query: String) {
        viewModelScope.launch {
            try {
                isSearching = true
                searchError = null

                // Check cache first
                val cacheKey = "doc:$query"
                val cachedEntry = searchCache[cacheKey]

                if (cachedEntry != null && !cachedEntry.isExpired()) {
                    Timber.tag(tag).d("Using cached document search results for: $query")
                    searchResults = cachedEntry.results
                    isSearching = false
                    return@launch
                }

                Timber.tag(tag).d("Searching documents for: $query")

                // Perform semantic search on indexed documents
                // For now, we'll do a simple text-based search
                // In the future, this could use embeddings for semantic similarity
                val results = androidSearchService.searchDocuments(query)

                searchResults = results.map { doc ->
                    mapOf(
                        "title" to "Document ${doc.id}",
                        "snippet" to (doc.text.take(200) + if (doc.text.length > 200) "..." else ""),
                        "link" to "document://${doc.id}",
                        "type" to "document"
                    )
                }

                // Cache the results
                cacheSearchResults(cacheKey, searchResults, query)

                Timber.tag(tag).d("Document search completed with ${searchResults.size} results")

            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error searching documents")
                ErrorHandler.reportError(e, "Document Search", ErrorHandler.ErrorSeverity.MEDIUM, "Document search failed. Please try again.")
                searchError = "Document search failed: ${e.message}"
                searchResults = emptyList()
            } finally {
                isSearching = false
            }
        }
    }

    // Android system search
    fun performAndroidSearch(query: String) {
        viewModelScope.launch {
            try {
                isSearching = true
                searchError = null

                Timber.tag(tag).d("Performing Android system search for: $query")

                // Use AndroidSearchService to launch browser search
                val response = androidSearchService.launchBrowserSearch(query)

                if (response.success && response.results?.isNotEmpty() == true) {
                    searchResults = response.results.map { result ->
                        mapOf(
                            "title" to result.title,
                            "snippet" to result.snippet,
                            "link" to result.url,
                            "type" to "browser"
                        )
                    }
                    Timber.tag(tag).d("Android search completed with ${searchResults.size} results")
                } else {
                    searchError = response.error ?: "Android search failed"
                    searchResults = emptyList()
                }

            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error performing Android search")
                ErrorHandler.reportError(e, "Android Search", ErrorHandler.ErrorSeverity.MEDIUM, "Android search failed. Please check if a browser is installed and try again.")
                searchError = "Android search failed: ${e.message}"
                searchResults = emptyList()
            } finally {
                isSearching = false
            }
        }
    }

    // Clear search results and cache
    fun clearSearchResults() {
        searchResults = emptyList()
        searchError = null
        searchCache.clear() // Clear cache as well
        Timber.tag(tag).d("Search results and cache cleared")
    }

    // Cache search results with automatic cleanup
    private fun cacheSearchResults(cacheKey: String, results: List<Map<String, String>>, query: String) {
        // Remove expired entries to prevent memory leaks
        val expiredKeys = searchCache.entries.filter { it.value.isExpired() }.map { it.key }
        expiredKeys.forEach { searchCache.remove(it) }

        // If cache is too large, remove oldest entries
        if (searchCache.size >= maxCacheSize) {
            val oldestEntries = searchCache.entries.sortedBy { it.value.timestamp }.take(searchCache.size - maxCacheSize + 1)
            oldestEntries.forEach { searchCache.remove(it.key) }
        }

        // Add new entry
        searchCache[cacheKey] = SearchCacheEntry(results, System.currentTimeMillis(), query)
        Timber.tag(tag).d("Cached search results for: $cacheKey")
    }

    // Get search result summary
    fun getSearchSummary(): String {
        return if (searchResults.isNotEmpty()) {
            "Found ${searchResults.size} results"
        } else if (searchError != null) {
            "Search error: $searchError"
        } else {
            "No search results"
        }
    }

    // Summarize search results (cached for performance)
    private var cachedSummary: String? = null

    fun summarizeSearchResults(): String {
        if (searchResults.isEmpty()) {
            return "No results to summarize"
        }

        // Return cached summary if available and results haven't changed
        val currentHash = searchResults.hashCode()
        if (cachedSummary != null && lastSummaryHash == currentHash) {
            return cachedSummary!!
        }

        val summary = StringBuilder()
        summary.append("🔍 **Search Summary**\n\n")
        summary.append("Found ${searchResults.size} results for your query.\n\n")

        searchResults.take(3).forEachIndexed { index, result ->
            summary.append("${index + 1}. **${result["title"]}**\n")
            summary.append("${result["snippet"]}\n\n")
        }

        if (searchResults.size > 3) {
            summary.append("... and ${searchResults.size - 3} more results")
        }

        cachedSummary = summary.toString()
        lastSummaryHash = currentHash
        return cachedSummary!!
    }

    private var lastSummaryHash = 0

    /**
     * Enhanced cleanup with memory optimization
     */
    fun cleanup() {
        // Clear cached data to free memory
        cachedSummary = null
        lastSummaryHash = 0

        // Clear search results if they are large
        if (searchResults.size > 50) {
            searchResults = emptyList()
        }

        searchError = null
        Timber.tag(tag).d("SearchViewModel cleaned up")
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}
