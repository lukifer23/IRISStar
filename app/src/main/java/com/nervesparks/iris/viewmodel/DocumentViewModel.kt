package com.nervesparks.iris.viewmodel

import timber.log.Timber
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.llama.cpp.LLamaAndroid
import com.nervesparks.iris.data.DocumentRepository
import com.nervesparks.iris.data.exceptions.ErrorHandler
import com.nervesparks.iris.data.exceptions.ValidationException
import com.nervesparks.iris.llm.EmbeddingService
import com.nervesparks.iris.llm.performDocumentIndexing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * PHASE 1.1.1: DocumentViewModel - Extracted from MainViewModel
 * Handles document processing, indexing, and summarization
 */
@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val llamaAndroid: LLamaAndroid,
    private val documentRepository: DocumentRepository,
    private val embeddingService: EmbeddingService,
    private val errorHandler: ErrorHandler
) : ViewModel() {

    var isDocumentIndexing by mutableStateOf(false)
        private set

    var documentIndexingError by mutableStateOf<String?>(null)
        private set

    var documentIndexingSuccess by mutableStateOf<String?>(null)
        private set

    var isSummarizing by mutableStateOf(false)
        private set

    var summarizationError by mutableStateOf<String?>(null)
        private set

    var summarizationResult by mutableStateOf<String?>(null)
        private set

    /**
     * Index a document for later retrieval
     */
    fun indexDocument(text: String) {
        if (text.isBlank()) {
            documentIndexingError = "Document text cannot be empty"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                isDocumentIndexing = true
                documentIndexingError = null
                documentIndexingSuccess = null
            }

            try {
                performDocumentIndexing(text, embeddingService, documentRepository)
                withContext(Dispatchers.Main) {
                    documentIndexingSuccess = "Document indexed successfully"
                }
            } catch (e: ValidationException) {
                Timber.e(e, "Validation error while indexing document")
                val errorMessage = e.message ?: "Failed to index document"
                withContext(Dispatchers.Main) {
                    documentIndexingError = errorMessage
                }
                errorHandler.reportError(e, "Document indexing failed")
            } catch (e: Exception) {
                Timber.e(e, "Failed to index document")
                val errorMessage = e.message ?: "Failed to index document"
                withContext(Dispatchers.Main) {
                    documentIndexingError = errorMessage
                }
                errorHandler.reportError(e, "Document indexing failed")
            } finally {
                withContext(Dispatchers.Main) {
                    isDocumentIndexing = false
                }
            }
        }
    }

    /**
     * Summarize document text
     */
    fun summarizeDocument(text: String) {
        if (text.isBlank()) {
            summarizationError = "Document text cannot be empty"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                isSummarizing = true
                summarizationError = null
                summarizationResult = null
            }

            try {
                // Use LLM to generate a proper summary
                val prompt = "Please provide a concise summary of the following text in 2-3 sentences:\n\n$text"
                val summary = generateLLMSummary(prompt)
                withContext(Dispatchers.Main) {
                    summarizationResult = summary
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to summarize document")
                val errorMessage = e.message ?: "Failed to summarize document"
                withContext(Dispatchers.Main) {
                    summarizationError = errorMessage
                }
                errorHandler.reportError(e, "Document summarization failed")
            } finally {
                withContext(Dispatchers.Main) {
                    isSummarizing = false
                }
            }
        }
    }

    /**
     * Clear all document-related state
     */
    fun clearDocumentState() {
        documentIndexingError = null
        documentIndexingSuccess = null
        summarizationError = null
        summarizationResult = null
    }

    /**
     * Search documents using embeddings
     */
    suspend fun searchDocuments(query: String): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Generate embedding for the query
                val queryEmbedding = embeddingService.embed(query)

                // Find similar documents
                val similarDocs = documentRepository.topKSimilar(queryEmbedding, 5)

                // Return the document contents
                similarDocs.map { it.text }
            } catch (e: Exception) {
                Timber.e(e, "Failed to search documents")
                emptyList<String>()
            }
        }
    }

    /**
     * Generate a summary using the LLM
     */
    private suspend fun generateLLMSummary(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // Send the prompt to LLM and collect all tokens
                val responseBuilder = StringBuilder()
                llamaAndroid.send(prompt).collect { token ->
                    responseBuilder.append(token)
                }

                // Clean up the response (remove any system prompts, etc.)
                responseBuilder.toString().trim()
            } catch (e: Exception) {
                Timber.e(e, "Failed to generate LLM summary")
                // Fallback to simple extraction
                "Summary: ${prompt.take(100)}${if (prompt.length > 100) "..." else ""}"
            }
        }
    }
}
