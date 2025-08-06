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
     * Perform a web search using a more reliable search API
     */
    suspend fun searchWeb(query: String): SearchResponse = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Searching web for: $query")
            
            // Use a more reliable search approach
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1&t=IrisAI"
            
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (compatible; IrisAI/1.0)")
                .addHeader("Accept", "application/json")
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
                Log.d(tag, "Raw API response: $jsonString")
                
                // Check if response is empty or contains test data
                if (jsonString.contains("\"production_state\":\"offline\"") || 
                    jsonString.contains("\"Just Another Test\"") ||
                    jsonString.isEmpty()) {
                    Log.d(tag, "Detected test API response, using fallback search")
                    return@withContext performFallbackSearch(query)
                }
                
                val json = JSONObject(jsonString)
                val results = mutableListOf<SearchResult>()
                
                // Extract Abstract (main result)
                if (json.has("Abstract") && json.getString("Abstract").isNotEmpty()) {
                    Log.d(tag, "Found Abstract: ${json.getString("Abstract")}")
                    results.add(SearchResult(
                        title = json.getString("AbstractSource"),
                        snippet = json.getString("Abstract"),
                        url = json.getString("AbstractURL"),
                        source = "DuckDuckGo Abstract"
                    ))
                } else {
                    Log.d(tag, "No Abstract found in response")
                }
                
                // Extract Related Topics
                if (json.has("RelatedTopics")) {
                    Log.d(tag, "Found RelatedTopics array")
                    val relatedTopics = json.getJSONArray("RelatedTopics")
                    Log.d(tag, "RelatedTopics length: ${relatedTopics.length()}")
                    for (i in 0 until minOf(relatedTopics.length(), 3)) {
                        val topic = relatedTopics.getJSONObject(i)
                        if (topic.has("Text")) {
                            Log.d(tag, "Found RelatedTopic: ${topic.getString("Text")}")
                            results.add(SearchResult(
                                title = topic.getString("FirstURL").substringAfterLast("/"),
                                snippet = topic.getString("Text"),
                                url = topic.getString("FirstURL"),
                                source = "DuckDuckGo Related"
                            ))
                        }
                    }
                } else {
                    Log.d(tag, "No RelatedTopics found in response")
                }
                
                // Extract Answer (if available)
                if (json.has("Answer") && json.getString("Answer").isNotEmpty()) {
                    Log.d(tag, "Found Answer: ${json.getString("Answer")}")
                    results.add(SearchResult(
                        title = "Direct Answer",
                        snippet = json.getString("Answer"),
                        url = json.getString("AbstractURL"),
                        source = "DuckDuckGo Answer"
                    ))
                } else {
                    Log.d(tag, "No Answer found in response")
                }

                Log.d(tag, "Found ${results.size} search results")
                
                // If no results from DuckDuckGo, try a fallback approach
                if (results.isEmpty()) {
                    Log.d(tag, "No results from DuckDuckGo, trying fallback search")
                    return@withContext performFallbackSearch(query)
                }
                
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
     * Fallback search method that provides basic information and search tips
     */
    private suspend fun performFallbackSearch(query: String): SearchResponse {
        Log.d(tag, "Performing fallback search for: $query")
        
        // Create a more helpful fallback response
        val results = listOf(
            SearchResult(
                title = "Search Information",
                snippet = "I performed a web search for \"$query\" but couldn't retrieve specific results at this time. This might be due to API limitations or the search query format. You can try:\n\n• Rephrasing your search with more specific keywords\n• Using simpler, shorter search terms\n• Checking your internet connection\n• Trying again in a few moments",
                url = "https://duckduckgo.com/?q=${URLEncoder.encode(query, "UTF-8")}",
                source = "Search Assistant"
            ),
            SearchResult(
                title = "Alternative Search",
                snippet = "You can also try searching directly on DuckDuckGo, Google, or other search engines for more comprehensive results.",
                url = "https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}",
                source = "Search Tips"
            ),
            SearchResult(
                title = "Search Query Analysis",
                snippet = "Your search: \"$query\"\n\nThis appears to be a ${if (query.contains("weather")) "weather" else if (query.contains("news")) "news" else "general"} search. For better results, try using specific location names, dates, or more targeted keywords.",
                url = "https://duckduckgo.com/",
                source = "Query Analysis"
            )
        )
        
        return SearchResponse(
            success = true,
            results = results,
            query = query
        )
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