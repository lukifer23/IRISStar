package com.nervesparks.iris.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Service for performing web searches using DuckDuckGo Instant Answer API
 * This provides free, privacy-focused search results
 */
class WebSearchService {
    private val client = OkHttpClient()
    private val tag = "WebSearchService"

    data class SearchResult(
        val title: String,
        val snippet: String,
        val url: String,
        val source: String
    )

    data class SearchResponse(
        val success: Boolean,
        val results: List<SearchResult>? = null,
        val error: String? = null,
        val query: String = ""
    )

    /**
     * Perform a web search using DuckDuckGo Instant Answer API
     */
    suspend fun searchWeb(query: String): SearchResponse = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Searching web for: $query")
            
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1"
            
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "IrisAI/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(tag, "Search failed with code: ${response.code}")
                    return@withContext SearchResponse(
                        success = false,
                        error = "Search failed: HTTP ${response.code}"
                    )
                }

                val jsonString = response.body?.string() ?: ""
                val json = JSONObject(jsonString)
                
                val results = mutableListOf<SearchResult>()
                
                // Extract Abstract (main result)
                if (json.has("Abstract") && json.getString("Abstract").isNotEmpty()) {
                    results.add(SearchResult(
                        title = json.getString("AbstractSource"),
                        snippet = json.getString("Abstract"),
                        url = json.getString("AbstractURL"),
                        source = "DuckDuckGo Abstract"
                    ))
                }
                
                // Extract Related Topics
                if (json.has("RelatedTopics")) {
                    val relatedTopics = json.getJSONArray("RelatedTopics")
                    for (i in 0 until minOf(relatedTopics.length(), 3)) {
                        val topic = relatedTopics.getJSONObject(i)
                        if (topic.has("Text")) {
                            results.add(SearchResult(
                                title = topic.getString("FirstURL").substringAfterLast("/"),
                                snippet = topic.getString("Text"),
                                url = topic.getString("FirstURL"),
                                source = "DuckDuckGo Related"
                            ))
                        }
                    }
                }
                
                // Extract Answer (if available)
                if (json.has("Answer") && json.getString("Answer").isNotEmpty()) {
                    results.add(SearchResult(
                        title = "Direct Answer",
                        snippet = json.getString("Answer"),
                        url = json.getString("AbstractURL"),
                        source = "DuckDuckGo Answer"
                    ))
                }

                Log.d(tag, "Found ${results.size} search results")
                return@withContext SearchResponse(
                    success = true,
                    results = results,
                    query = query
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error performing web search", e)
            return@withContext SearchResponse(
                success = false,
                error = "Search error: ${e.message}"
            )
        }
    }

    /**
     * Format search results for display in chat
     */
    fun formatSearchResults(results: List<SearchResult>, query: String): String {
        if (results.isEmpty()) {
            return "I couldn't find any relevant information for \"$query\". Please try rephrasing your search."
        }

        val sb = StringBuilder()
        sb.append("🔍 **Search Results for \"$query\"**\n\n")
        
        results.forEachIndexed { index, result ->
            sb.append("**${index + 1}. ${result.title}**\n")
            sb.append("${result.snippet}\n")
            sb.append("Source: ${result.url}\n")
            sb.append("---\n\n")
        }
        
        sb.append("These results were found using web search. Please verify any important information.")
        return sb.toString()
    }
} 