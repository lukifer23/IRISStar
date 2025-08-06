package com.nervesparks.iris.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class WebSearchService {
    private val client = OkHttpClient()
    private val tag = "WebSearchService"
    
    // Google Custom Search API credentials
    // You'll need to get these from Google Cloud Console
    private val GOOGLE_API_KEY = "AIzaSyAoFgCVdXg6lOsHVPVVd4en9I5m1WusmiY" // Replace with your API key
    private val GOOGLE_CSE_ID = "017576662512468239146:omuauf_lfve" // Default search engine ID
    
    // Fallback to DuckDuckGo if Google API not configured
    private val useGoogleAPI = GOOGLE_API_KEY != "YOUR_GOOGLE_API_KEY" && GOOGLE_CSE_ID != "YOUR_CUSTOM_SEARCH_ENGINE_ID"

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
     * Perform a web search using Google Custom Search API or fallback to DuckDuckGo
     */
    suspend fun searchWeb(query: String): SearchResponse = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Searching web for: $query")
            
            if (useGoogleAPI) {
                performGoogleSearch(query)
            } else {
                performDuckDuckGoSearch(query)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error performing web search", e)
            SearchResponse(
                success = false,
                error = "Search error: ${e.message}"
            )
        }
    }
    
    /**
     * Perform search using Google Custom Search API
     */
    private suspend fun performGoogleSearch(query: String): SearchResponse = withContext(Dispatchers.IO) {
        Log.d(tag, "Using Google Custom Search API")
        
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://www.googleapis.com/customsearch/v1?key=$GOOGLE_API_KEY&cx=$GOOGLE_CSE_ID&q=$encodedQuery&num=5"
        
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "IrisAI/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(tag, "Google search failed with code: ${response.code}")
                SearchResponse(
                    success = false,
                    error = "Google search failed: HTTP ${response.code}"
                )
            } else {
                val jsonString = response.body?.string() ?: ""
                Log.d(tag, "Google API response: $jsonString")
                
                val json = JSONObject(jsonString)
                val results = mutableListOf<SearchResult>()
                
                if (json.has("items")) {
                    val items = json.getJSONArray("items")
                    Log.d(tag, "Found ${items.length()} Google search results")
                    
                    for (i in 0 until minOf(items.length(), 5)) {
                        val item = items.getJSONObject(i)
                        results.add(SearchResult(
                            title = item.getString("title"),
                            snippet = item.getString("snippet"),
                            url = item.getString("link"),
                            source = "Google Search"
                        ))
                    }
                } else {
                    Log.d(tag, "No items found in Google search response")
                }
                
                SearchResponse(
                    success = true,
                    results = results,
                    query = query
                )
            }
        }
    }
    
    /**
     * Fallback search using DuckDuckGo (with better error handling)
     */
    private suspend fun performDuckDuckGoSearch(query: String): SearchResponse = withContext(Dispatchers.IO) {
        Log.d(tag, "Using DuckDuckGo fallback search")
        
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1&t=IrisAI"
        
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (compatible; IrisAI/1.0)")
            .addHeader("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(tag, "DuckDuckGo search failed with code: ${response.code}")
                SearchResponse(
                    success = false,
                    error = "DuckDuckGo search failed: HTTP ${response.code}"
                )
            } else {
                val jsonString = response.body?.string() ?: ""
                Log.d(tag, "DuckDuckGo API response: $jsonString")
                
                // Check if response is empty or contains test data
                if (jsonString.contains("\"production_state\":\"offline\"") || 
                    jsonString.contains("\"Just Another Test\"") ||
                    jsonString.isEmpty()) {
                    Log.d(tag, "Detected test API response, using fallback search")
                    performFallbackSearch(query)
                } else {
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

                    Log.d(tag, "Found ${results.size} DuckDuckGo search results")
                    
                    // If no results from DuckDuckGo, try a fallback approach
                    if (results.isEmpty()) {
                        Log.d(tag, "No results from DuckDuckGo, trying fallback search")
                        performFallbackSearch(query)
                    } else {
                        SearchResponse(
                            success = true,
                            results = results,
                            query = query
                        )
                    }
                }
            }
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
     * Format search results for display
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