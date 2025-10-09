package com.nervesparks.iris.viewmodel

import timber.log.Timber
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nervesparks.iris.Downloadable
import com.nervesparks.iris.data.HuggingFaceApiService
import com.nervesparks.iris.data.exceptions.ErrorHandler
import com.nervesparks.iris.data.repository.ModelRepository
import com.nervesparks.iris.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * PHASE 1.1.4: DownloadViewModel - Extracted from MainViewModel
 * Handles model downloads, search, and download queue management
 */
@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val huggingFaceApiService: HuggingFaceApiService,
    private val modelRepository: ModelRepository,
    private val userPreferencesRepository: com.nervesparks.iris.data.UserPreferencesRepository,
    private val errorHandler: ErrorHandler
) : ViewModel() {

    // Download state
    var downloadableModels by mutableStateOf<List<Downloadable>>(emptyList())
        private set

    var isSearchingModels by mutableStateOf(false)
        private set

    var searchError by mutableStateOf<String?>(null)
        private set

    var isDownloading by mutableStateOf(false)
        private set

    var downloadProgress by mutableStateOf(0f)
        private set

    var downloadError by mutableStateOf<String?>(null)
        private set

    var downloadSuccess by mutableStateOf<String?>(null)
        private set

    var currentDownloadFile by mutableStateOf<String?>(null)
        private set

    // Download queue management
    private val downloadQueue = mutableListOf<Downloadable>()
    private var currentDownloadJob: kotlinx.coroutines.Job? = null

    /**
     * Search for models on HuggingFace
     */
    fun searchModels(query: String) {
        if (query.isBlank()) {
            searchError = "Search query cannot be empty"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                isSearchingModels = true
                searchError = null
            }

            try {
                val models = huggingFaceApiService.searchModels(query)
                val downloadable = models.map { model ->
                    // For now, create placeholder downloadables - actual URLs would need to be constructed
                    // based on model files, but this requires additional API calls
                    Downloadable(
                        name = model.id,
                        source = android.net.Uri.parse("https://huggingface.co/${model.id}"),
                        destination = File("", "${model.id}.gguf") // Placeholder, will be set by caller
                    )
                }

                withContext(Dispatchers.Main) {
                    downloadableModels = downloadable
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to search models")
                val errorMessage = e.message ?: "Failed to search models"
                withContext(Dispatchers.Main) {
                    searchError = errorMessage
                }
                errorHandler.reportError(e, "Model search failed")
            } finally {
                withContext(Dispatchers.Main) {
                    isSearchingModels = false
                }
            }
        }
    }

    /**
     * Load existing models from directory
     */
    fun loadExistingModels(directory: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val models = directory.listFiles { file ->
                    file.isFile && file.name.endsWith(".gguf", ignoreCase = true)
                }?.map { file ->
                    Downloadable(
                        name = file.name,
                        source = android.net.Uri.parse(""), // Not applicable for existing models
                        destination = file
                    )
                } ?: emptyList()

                withContext(Dispatchers.Main) {
                    downloadableModels = models
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load existing models")
                errorHandler.reportError(e, "Failed to load existing models")
            }
        }
    }

    /**
     * Download a model file
     */
    fun downloadModel(downloadable: Downloadable) {
        // Add to queue if already downloading
        if (isDownloading) {
            downloadQueue.add(downloadable)
            return
        }

        currentDownloadJob = viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                isDownloading = true
                downloadProgress = 0f
                downloadError = null
                downloadSuccess = null
                currentDownloadFile = downloadable.name
            }

            try {
                // TODO: Implement actual download logic with progress tracking
                // For now, simulate download
                for (i in 0..100) {
                    // Check for cancellation
                    if (!isActive) break

                    withContext(Dispatchers.Main) {
                        downloadProgress = i / 100f
                    }
                    kotlinx.coroutines.delay(50) // Simulate progress
                }

                if (isActive) {
                    withContext(Dispatchers.Main) {
                        downloadSuccess = "Downloaded ${downloadable.name}"
                    }

                    // Process next item in queue
                    processNextInQueue()
                }
            } catch (e: Exception) {
                if (isActive) { // Only report error if not cancelled
                    Timber.e(e, "Failed to download model")
                    val errorMessage = e.message ?: "Failed to download model"
                    withContext(Dispatchers.Main) {
                        downloadError = errorMessage
                    }
                    errorHandler.reportError(e, "Model download failed")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isDownloading = false
                    currentDownloadFile = null
                }
            }
        }
    }

    /**
     * Cancel current download
     */
    fun cancelDownload() {
        currentDownloadJob?.cancel()
        currentDownloadJob = null

        // Update state directly (since we're not in a coroutine)
        isDownloading = false
        downloadProgress = 0f
        downloadError = "Download cancelled"
        currentDownloadFile = null

        // Clear the queue as well
        downloadQueue.clear()
    }

    /**
     * Process next item in download queue
     */
    private fun processNextInQueue() {
        if (downloadQueue.isNotEmpty()) {
            val nextDownload = downloadQueue.removeAt(0)
            downloadModel(nextDownload)
        }
    }

    /**
     * Set test HuggingFace token (for development)
     */
    fun setTestHuggingFaceToken(token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userPreferencesRepository.huggingFaceToken = token
            } catch (e: Exception) {
                Timber.e(e, "Failed to set HuggingFace token")
                errorHandler.reportError(e, "Failed to set HuggingFace token")
            }
        }
    }

    /**
     * Clear all download-related state
     */
    fun clearDownloadState() {
        downloadError = null
        downloadSuccess = null
        searchError = null
        downloadProgress = 0f
        currentDownloadFile = null
    }

    /**
     * Get download queue status
     */
    fun getDownloadQueueStatus(): Map<String, Any> {
        return mapOf(
            "isDownloading" to isDownloading,
            "currentFile" to (currentDownloadFile ?: ""),
            "progress" to downloadProgress,
            "queueSize" to downloadQueue.size,
            "queuedFiles" to downloadQueue.map { it.name }
        )
    }
}
