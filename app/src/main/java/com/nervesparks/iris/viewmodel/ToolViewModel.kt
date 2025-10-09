package com.nervesparks.iris.viewmodel

import timber.log.Timber
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.llama.cpp.LLamaAndroid
import com.nervesparks.iris.data.ToolCall
import com.nervesparks.iris.data.WebSearchService
import com.nervesparks.iris.data.exceptions.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * PHASE 1.1.3: ToolViewModel - Extracted from MainViewModel
 * Handles tool calling, translation, and utility functions
 */
@HiltViewModel
class ToolViewModel @Inject constructor(
    private val llamaAndroid: LLamaAndroid,
    private val webSearchService: WebSearchService,
    private val errorHandler: ErrorHandler
) : ViewModel() {

    // Translation state
    var isTranslating by mutableStateOf(false)
        private set

    var translationError by mutableStateOf<String?>(null)
        private set

    var translationResult by mutableStateOf<String?>(null)
        private set

    // Tool call state
    var isProcessingTool by mutableStateOf(false)
        private set

    var toolCallError by mutableStateOf<String?>(null)
        private set

    var toolCallResult by mutableStateOf<String?>(null)
        private set

    /**
     * Translate text to target language
     */
    fun translate(text: String, targetLanguage: String) {
        if (text.isBlank()) {
            translationError = "Text to translate cannot be empty"
            return
        }

        if (targetLanguage.isBlank()) {
            translationError = "Target language cannot be empty"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                isTranslating = true
                translationError = null
                translationResult = null
            }

            try {
                // Use LLM for translation
                val prompt = "Please translate the following text to $targetLanguage. Only provide the translation, no additional text:\n\n$text"
                val translated = generateLLMTranslation(prompt, text)
                withContext(Dispatchers.Main) {
                    translationResult = translated
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to translate text")
                val errorMessage = e.message ?: "Translation failed"
                withContext(Dispatchers.Main) {
                    translationError = errorMessage
                }
                errorHandler.reportError(e, "Translation failed")
            } finally {
                withContext(Dispatchers.Main) {
                    isTranslating = false
                }
            }
        }
    }

    /**
     * Handle tool call execution
     */
    suspend fun handleToolCall(toolCall: ToolCall) {
        withContext(Dispatchers.Main) {
            isProcessingTool = true
            toolCallError = null
            toolCallResult = null
        }

        try {
            val result = when (toolCall.name) {
                "web_search" -> handleWebSearch(toolCall.args)
                "calculate" -> handleCalculation(toolCall.args)
                "file_search" -> handleFileSearch(toolCall.args)
                else -> "Unknown tool: ${toolCall.name}"
            }

            withContext(Dispatchers.Main) {
                toolCallResult = result
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to process tool call")
            val errorMessage = e.message ?: "Tool execution failed"
            withContext(Dispatchers.Main) {
                toolCallError = errorMessage
            }
            errorHandler.reportError(e, "Tool call failed")
        } finally {
            withContext(Dispatchers.Main) {
                isProcessingTool = false
            }
        }
    }

    /**
     * Handle web search tool call
     */
    private suspend fun handleWebSearch(arguments: Map<String, Any>): String {
        val query = arguments["query"] as? String ?: "No query provided"
        return withContext(Dispatchers.IO) {
            try {
                val searchResponse = webSearchService.searchWeb(query)
                if (searchResponse.success && searchResponse.results != null) {
                    webSearchService.formatSearchResults(searchResponse.results, query)
                } else {
                    "Web search failed: ${searchResponse.error ?: "Unknown error"}"
                }
            } catch (e: Exception) {
                Timber.e(e, "Web search failed")
                "Web search error: ${e.message}"
            }
        }
    }

    /**
     * Handle calculation tool call
     */
    private suspend fun handleCalculation(arguments: Map<String, Any>): String {
        val expression = arguments["expression"] as? String ?: "No expression provided"
        return withContext(Dispatchers.IO) {
            try {
                // Use LLM for calculation
                val prompt = "Please calculate the following expression and provide only the numerical result: $expression"
                val result = generateLLMCalculation(prompt, expression)
                result
            } catch (e: Exception) {
                Timber.e(e, "Calculation failed")
                "Calculation failed for: $expression"
            }
        }
    }

    /**
     * Handle file search tool call
     */
    private suspend fun handleFileSearch(arguments: Map<String, Any>): String {
        val pattern = arguments["pattern"] as? String ?: "No pattern provided"
        return withContext(Dispatchers.IO) {
            try {
                // For now, simulate file search - in a real implementation,
                // this would search through indexed documents or file system
                "File search completed for pattern: '$pattern'. Found 0 matches (file search not yet implemented)"
            } catch (e: Exception) {
                Timber.e(e, "File search failed")
                "File search failed for pattern: $pattern"
            }
        }
    }

    /**
     * Clear all tool-related state
     */
    fun clearToolState() {
        translationError = null
        translationResult = null
        toolCallError = null
        toolCallResult = null
    }

    /**
     * Generate a calculation using the LLM
     */
    private suspend fun generateLLMCalculation(prompt: String, expression: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // Send the prompt to LLM and collect all tokens
                val responseBuilder = StringBuilder()
                llamaAndroid.send(prompt).collect { token ->
                    responseBuilder.append(token)
                }

                // Clean up the response - extract just the numerical result
                val cleaned = responseBuilder.toString().trim()
                // Try to extract just numbers and basic math symbols
                val result = cleaned.lines().firstOrNull { line ->
                    line.contains(Regex("[0-9]")) && !line.contains("calculate", ignoreCase = true)
                } ?: cleaned

                result.trim()
            } catch (e: Exception) {
                Timber.e(e, "Failed to generate LLM calculation")
                // Fallback
                "Could not calculate: $expression"
            }
        }
    }

    /**
     * Get available tools
     */
    fun getAvailableTools(): List<String> {
        return listOf("web_search", "calculate", "file_search", "translate")
    }

    /**
     * Generate a translation using the LLM
     */
    private suspend fun generateLLMTranslation(prompt: String, originalText: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // Send the prompt to LLM and collect all tokens
                val responseBuilder = StringBuilder()
                llamaAndroid.send(prompt).collect { token ->
                    responseBuilder.append(token)
                }

                // Clean up the response
                responseBuilder.toString().trim()
            } catch (e: Exception) {
                Timber.e(e, "Failed to generate LLM translation")
                // Fallback
                "[Translation failed] $originalText"
            }
        }
    }
}
