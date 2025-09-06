package com.nervesparks.iris.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nervesparks.iris.data.WebSearchService
import com.nervesparks.iris.data.AndroidSearchService
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

    // Web search function
    fun performWebSearch(query: String, summarize: Boolean = true) {
        viewModelScope.launch {
            try {
                isSearching = true
                searchError = null

                Log.d(tag, "Performing web search for: $query")
                val response = webSearchService.searchWeb(query)

                if (response.success && response.results?.isNotEmpty() == true) {
                    searchResults = response.results.map { result ->
                        mapOf(
                            "title" to result.title,
                            "snippet" to result.snippet,
                            "link" to result.url
                        )
                    }

                    if (summarize && searchResults.isNotEmpty()) {
                        // TODO: Implement summarization
                        Log.d(tag, "Search completed with ${searchResults.size} results")
                    }
                } else {
                    searchResults = emptyList()
                    searchError = response.error ?: "No search results found"
                }

            } catch (e: Exception) {
                Log.e(tag, "Error performing web search", e)
                searchError = "Search failed: ${e.message}"
                searchResults = emptyList()
            } finally {
                isSearching = false
            }
        }
    }

    // Document search function
    fun searchDocuments(query: String) {
        viewModelScope.launch {
            try {
                isSearching = true
                searchError = null

                Log.d(tag, "Searching documents for: $query")
                // TODO: Implement document search
                // This would integrate with documentRepository for semantic search

                Log.d(tag, "Document search completed")

            } catch (e: Exception) {
                Log.e(tag, "Error searching documents", e)
                searchError = "Document search failed: ${e.message}"
            } finally {
                isSearching = false
            }
        }
    }

    // Android system search
    fun performAndroidSearch(query: String) {
        viewModelScope.launch {
            try {
                Log.d(tag, "Performing Android search for: $query")
                // TODO: Implement Android system search
                Log.d(tag, "Android search not yet implemented")
            } catch (e: Exception) {
                Log.e(tag, "Error performing Android search", e)
            }
        }
    }

    // Clear search results
    fun clearSearchResults() {
        searchResults = emptyList()
        searchError = null
        Log.d(tag, "Search results cleared")
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
}
