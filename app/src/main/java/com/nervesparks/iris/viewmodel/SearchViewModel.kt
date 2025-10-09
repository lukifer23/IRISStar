package com.nervesparks.iris.viewmodel

import timber.log.Timber
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nervesparks.iris.data.AndroidSearcher
import com.nervesparks.iris.data.DocumentRepository
import com.nervesparks.iris.data.WebSearcher
import com.nervesparks.iris.data.search.SearchResult
import com.nervesparks.iris.llm.EmbeddingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PHASE 1.3: SearchViewModel - Extracted from MainViewModel
 * Handles web search, document search, and related functionality
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val webSearcher: WebSearcher,
    private val androidSearcher: AndroidSearcher,
    private val documentRepository: DocumentRepository,
    private val embeddingService: EmbeddingService
) : ViewModel() {

    private val tag = "SearchViewModel"

    // Search state
    var isSearching by mutableStateOf(false)
        private set
    var searchResults by mutableStateOf<List<SearchResult>>(emptyList())
        private set
    var webResults by mutableStateOf<List<SearchResult>>(emptyList())
        private set
    var documentResults by mutableStateOf<List<SearchResult>>(emptyList())
        private set
    var androidResults by mutableStateOf<List<SearchResult>>(emptyList())
        private set
    var searchError by mutableStateOf<String?>(null)
        private set
    var searchSummary by mutableStateOf<String?>(null)
        private set
    var searchStatusMessage by mutableStateOf("")
        private set
    var currentQuery by mutableStateOf("")
        private set

    // Web search function
    fun performWebSearch(query: String, summarize: Boolean = true) {
        viewModelScope.launch {
            try {
                isSearching = true
                prepareForSearch(query, "Searching web for \"$query\"...")

                Timber.tag(tag).d("Performing web search for: $query")

                // Try to retrieve matching documents to surface alongside web results
                documentResults = try {
                    fetchDocumentResults(query)
                } catch (e: Exception) {
                    Timber.tag(tag).e(e, "Document retrieval failed during web search")
                    val message = "Document search failed: ${e.message}".also { searchError = it }
                    searchStatusMessage = message
                    emptyList()
                }

                updateCombinedResults()

                val response = webSearcher.searchWeb(query)

                if (response.success && !response.results.isNullOrEmpty()) {
                    webResults = response.results
                    updateCombinedResults()

                    if (summarize) {
                        searchSummary = buildSummary(searchResults)
                        searchStatusMessage = searchSummary ?: "Found ${searchResults.size} results"
                    } else {
                        searchStatusMessage = "Found ${searchResults.size} results"
                    }

                    Timber.tag(tag).d("Search completed with ${searchResults.size} results")
                } else {
                    webResults = emptyList()
                    updateCombinedResults()
                    val message = response.error ?: "No search results found"
                    searchError = message
                    searchStatusMessage = message
                }

            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error performing web search")
                val message = "Search failed: ${e.message}"
                searchError = message
                webResults = emptyList()
                updateCombinedResults()
                searchStatusMessage = message
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
                prepareForSearch(query, "Searching documents for \"$query\"...")

                Timber.tag(tag).d("Searching documents for: $query")

                documentResults = fetchDocumentResults(query)
                updateCombinedResults()

                searchStatusMessage = if (documentResults.isNotEmpty()) {
                    "Found ${documentResults.size} matching documents"
                } else {
                    "No matching documents found"
                }

                Timber.tag(tag).d("Document search completed")

            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error searching documents")
                documentResults = emptyList()
                updateCombinedResults()
                val message = "Document search failed: ${e.message}"
                searchError = message
                searchStatusMessage = message
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
                prepareForSearch(query, "Launching Android search for \"$query\"...")

                Timber.tag(tag).d("Performing Android search for: $query")
                val response = androidSearcher.launchBrowserSearch(query)

                if (response.success && !response.results.isNullOrEmpty()) {
                    androidResults = response.results
                    updateCombinedResults()
                    searchStatusMessage = response.results.first().snippet
                } else {
                    androidResults = emptyList()
                    updateCombinedResults()
                    val message = response.error ?: "Android search returned no results"
                    searchError = message
                    searchStatusMessage = message
                }

                Timber.tag(tag).d("Android search completed")
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error performing Android search")
                androidResults = emptyList()
                updateCombinedResults()
                val message = "Android search failed: ${e.message}"
                searchError = message
                searchStatusMessage = message
            } finally {
                isSearching = false
            }
        }
    }

    // Clear search results
    fun clearSearchResults() {
        isSearching = false
        searchResults = emptyList()
        webResults = emptyList()
        documentResults = emptyList()
        androidResults = emptyList()
        searchError = null
        searchSummary = null
        searchStatusMessage = ""
        currentQuery = ""
        Timber.tag(tag).d("Search results cleared")
    }

    // Get search result summary
    fun getSearchSummary(): String {
        return searchSummary
            ?: when {
                searchResults.isNotEmpty() -> "Found ${searchResults.size} results"
                searchError != null -> "Search error: $searchError"
                else -> "No search results"
            }
    }

    private suspend fun fetchDocumentResults(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        val embedding = embeddingService.embed(query)
        val documents = documentRepository.topKSimilar(embedding, DOCUMENT_RESULT_LIMIT)

        return documents.map { document ->
            val snippet = document.text.trim().take(SUMMARY_SNIPPET_LIMIT)
            val formattedSnippet = if (document.text.length > SUMMARY_SNIPPET_LIMIT) {
                "$snippet…"
            } else {
                snippet
            }

            SearchResult(
                title = "Document ${document.id}",
                snippet = formattedSnippet,
                url = "document://${document.id}",
                source = "Indexed Document"
            )
        }
    }

    private fun buildSummary(results: List<SearchResult>): String? {
        if (results.isEmpty()) return null

        val highlights = results.take(3)
        val summaryLines = highlights.map { result ->
            val snippet = result.snippet.replace("\n", " ")
            "• ${result.title}: ${snippet.take(SUMMARY_SNIPPET_LIMIT)}"
        }

        return "Key takeaways:\n" + summaryLines.joinToString(separator = "\n")
    }

    private fun updateCombinedResults() {
        searchResults = documentResults + webResults + androidResults
    }

    private fun prepareForSearch(query: String, statusMessage: String) {
        searchError = null
        searchSummary = null
        currentQuery = query
        documentResults = emptyList()
        webResults = emptyList()
        androidResults = emptyList()
        updateCombinedResults()
        searchStatusMessage = statusMessage
    }

    companion object {
        private const val DOCUMENT_RESULT_LIMIT = 5
        private const val SUMMARY_SNIPPET_LIMIT = 160
    }
}
