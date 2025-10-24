package com.nervesparks.iris.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.nervesparks.iris.data.db.Document
import com.nervesparks.iris.data.exceptions.ValidationException
import com.nervesparks.iris.data.search.SearchResponse
import com.nervesparks.iris.data.search.SearchResult
import com.nervesparks.iris.llm.EmbeddingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject

/**
 * Android system integration for web search
 * This service can launch searches in the user's default browser
 */
class AndroidSearchService @Inject constructor(
    private val context: Context,
    private val documentRepository: DocumentRepository,
    private val embeddingService: EmbeddingService
) {
    private val tag = "AndroidSearchService"

    /**
     * Launch search in user's default browser
     */
    suspend fun launchBrowserSearch(query: String): SearchResponse<SearchResult> = withContext(Dispatchers.IO) {
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
                return@withContext SearchResponse<SearchResult>(
                    success = false,
                    error = "No browser app found to handle the search"
                )
            }
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error launching browser search")
            return@withContext SearchResponse<SearchResult>(
                success = false,
                error = "Failed to launch browser search: ${e.message}"
            )
        }
    }

    /**
     * Launch search with multiple search engines
     */
    suspend fun launchMultiSearch(query: String): SearchResponse<SearchResult> = withContext(Dispatchers.IO) {
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
            return@withContext SearchResponse<SearchResult>(
                success = false,
                error = "Failed to launch multi-search: ${e.message}"
            )
        }
    }

    /**
     * Format search results for display
     */
    fun formatSearchResults(results: List<SearchResult>, query: String): String {
        if (results.isEmpty()) {
            return "I couldn't launch any search for \"$query\". Please try again."
        }
        
        val sb = StringBuilder()
        sb.append("🔍 **Browser Search for \"$query\"**\n\n")
        
        results.forEachIndexed { index, result ->
            sb.append("**${index + 1}. ${result.title}**\n")
            sb.append("${result.snippet}\n")
            sb.append("URL: ${result.url}\n")
            result.confidence?.let {
                sb.append("Confidence: ${String.format(Locale.US, "%.2f", it)}\n")
            }
            sb.append("---\n\n")
        }
        
        sb.append("I've opened the search in your browser. You can copy relevant information back to our conversation.")
        return sb.toString()
    }

    /**
     * Search through indexed documents
     */
    suspend fun searchDocuments(query: String, desiredK: Int = DEFAULT_DOCUMENT_RESULT_COUNT): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            Timber.tag(tag).d("Searching documents for: $query")

            if (query.isBlank()) {
                Timber.tag(tag).w("Cannot search documents with a blank query")
                return@withContext emptyList<SearchResult>()
            }

            val queryEmbedding = embeddingService.embed(query)

            val documents = try {
                documentRepository.topKSimilar(queryEmbedding, desiredK)
            } catch (validation: ValidationException) {
                Timber.tag(tag).w(validation, "Validation error while searching documents")
                return@withContext emptyList<SearchResult>()
            }

            documents.map { document ->
                val similarity = cosineSimilarity(document.embedding, queryEmbedding)
                SearchResult(
                    title = buildTitle(document),
                    snippet = buildSnippet(document),
                    url = "document://${document.id}",
                    source = "Local Document",
                    confidence = similarity
                )
            }
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error searching documents")
            return@withContext emptyList<SearchResult>()
        }
    }

    private fun buildTitle(document: Document): String {
        val firstLine = document.text.lineSequence().firstOrNull()?.trim()
        return when {
            !firstLine.isNullOrEmpty() -> firstLine.take(MAX_TITLE_LENGTH)
            document.text.length <= MAX_TITLE_LENGTH -> document.text
            else -> "Document ${document.id}"
        }
    }

    private fun buildSnippet(document: Document): String {
        val snippet = document.text.take(MAX_SNIPPET_LENGTH)
        return if (document.text.length > MAX_SNIPPET_LENGTH) "$snippet..." else snippet
    }

    private fun cosineSimilarity(a: List<Float>, b: List<Float>): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val size = minOf(a.size, b.size)
        var dot = 0.0
        var magnitudeA = 0.0
        var magnitudeB = 0.0
        for (i in 0 until size) {
            dot += (a[i] * b[i])
            magnitudeA += (a[i] * a[i])
            magnitudeB += (b[i] * b[i])
        }
        val denominator = kotlin.math.sqrt(magnitudeA) * kotlin.math.sqrt(magnitudeB)
        return if (denominator == 0.0) 0f else (dot / denominator).toFloat()
    }

    companion object {
        private const val DEFAULT_DOCUMENT_RESULT_COUNT = 5
        private const val MAX_TITLE_LENGTH = 80
        private const val MAX_SNIPPET_LENGTH = 200
    }
}