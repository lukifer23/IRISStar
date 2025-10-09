package com.nervesparks.iris.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import timber.log.Timber
import com.nervesparks.iris.data.search.SearchResponse
import com.nervesparks.iris.data.search.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

/**
 * Abstraction describing the Android system search capabilities.
 */
interface AndroidSearcher {
    suspend fun launchBrowserSearch(query: String): SearchResponse
    suspend fun launchMultiSearch(query: String): SearchResponse
    fun formatSearchResults(results: List<SearchResult>, query: String): String
}

/**
 * Android system integration for web search
 * This service can launch searches in the user's default browser
 */
class AndroidSearchService(private val context: Context) : AndroidSearcher {
    private val tag = "AndroidSearchService"

    /**
     * Launch search in user's default browser
     */
    override suspend fun launchBrowserSearch(query: String): SearchResponse = withContext(Dispatchers.IO) {
        try {
            Timber.tag(tag).d("Launching browser search for: $query")
            
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://www.google.com/search?q=$encodedQuery"
            
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            // Check if there's an app to handle this intent
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                
                return@withContext SearchResponse(
                    success = true,
                    results = listOf(
                        SearchResult(
                            title = "Search Launched",
                            snippet = "I've opened your search \"$query\" in your default browser. You can view the results there and copy any relevant information back to our conversation.",
                            url = searchUrl,
                            source = "Android Browser"
                        )
                    ),
                    query = query
                )
            } else {
                return@withContext SearchResponse(
                    success = false,
                    error = "No browser app found to handle the search"
                )
            }
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error launching browser search")
            return@withContext SearchResponse(
                success = false,
                error = "Failed to launch browser search: ${e.message}"
            )
        }
    }

    /**
     * Launch search with multiple search engines
     */
    override suspend fun launchMultiSearch(query: String): SearchResponse = withContext(Dispatchers.IO) {
        try {
            Timber.tag(tag).d("Launching multi-search for: $query")
            
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchEngines = listOf(
                "Google" to "https://www.google.com/search?q=$encodedQuery",
                "DuckDuckGo" to "https://duckduckgo.com/?q=$encodedQuery",
                "Bing" to "https://www.bing.com/search?q=$encodedQuery"
            )
            
            val results = mutableListOf<SearchResult>()
            
            searchEngines.forEach { (name, url) ->
                results.add(
                    SearchResult(
                        title = "Search on $name",
                        snippet = "Click to search \"$query\" on $name",
                        url = url,
                        source = "Android Browser"
                    )
                )
            }
            
            // Launch the first search engine (Google) by default
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchEngines.first().second))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            }
            
            return@withContext SearchResponse(
                success = true,
                results = results,
                query = query
            )
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error launching multi-search")
            return@withContext SearchResponse(
                success = false,
                error = "Failed to launch multi-search: ${e.message}"
            )
        }
    }

    /**
     * Format search results for display
     */
    override fun formatSearchResults(results: List<SearchResult>, query: String): String {
        if (results.isEmpty()) {
            return "I couldn't launch any search for \"$query\". Please try again."
        }
        
        val sb = StringBuilder()
        sb.append("🔍 **Browser Search for \"$query\"**\n\n")
        
        results.forEachIndexed { index, result ->
            sb.append("**${index + 1}. ${result.title}**\n")
            sb.append("${result.snippet}\n")
            sb.append("URL: ${result.url}\n")
            sb.append("---\n\n")
        }
        
        sb.append("I've opened the search in your browser. You can copy relevant information back to our conversation.")
        return sb.toString()
    }
} 