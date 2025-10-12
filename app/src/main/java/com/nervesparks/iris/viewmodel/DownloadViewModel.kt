package com.nervesparks.iris.viewmodel

import android.content.Context
import android.net.Uri
import android.text.format.Formatter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nervesparks.iris.Downloadable
import com.nervesparks.iris.data.DownloadQueueItem
import com.nervesparks.iris.data.HuggingFaceApiService
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.data.exceptions.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlin.io.DEFAULT_BUFFER_SIZE

/**
 * PHASE 1.1.4: DownloadViewModel - Extracted from MainViewModel
 * Handles model downloads, search, and download queue management
 */
@HiltViewModel
class DownloadViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val huggingFaceApiService: HuggingFaceApiService,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val errorHandler: ErrorHandler,
    private val okHttpClient: OkHttpClient
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
    private val queueMutex = Mutex()
    private var currentDownloadJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            restorePersistedQueue()
        }
    }

    /**
     * Search for models on HuggingFace and surface concrete model files
     */
    fun searchModels(query: String, targetDirectory: File? = null) {
        if (query.isBlank()) {
            searchError = "Search query cannot be empty"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                isSearchingModels = true
                searchError = null
            }

            val authHeader = userPreferencesRepository.huggingFaceToken
                .takeIf { it.isNotBlank() }
                ?.let { "Bearer $it" }

            val destinationRoot = targetDirectory
                ?: appContext.getExternalFilesDir(null)
                ?: appContext.filesDir
            destinationRoot.mkdirs()

            try {
                val models = huggingFaceApiService.searchModels(query, authHeader)
                val detailed = coroutineScope {
                    models.map { model ->
                        async {
                            runCatching { huggingFaceApiService.getModelDetails(model.id, authHeader) }
                                .onFailure { Timber.e(it, "Failed to fetch model details for ${model.id}") }
                                .getOrDefault(model)
                        }
                    }.awaitAll()
                }

                val downloadables = detailed.flatMap { info ->
                    info.siblings.mapNotNull { file ->
                        val sanitizedFilename = file.filename.trimStart('/')
                        if (sanitizedFilename.isBlank()) return@mapNotNull null
                        val downloadUrl = buildDownloadUrl(info.id, sanitizedFilename)
                        val destinationFile = File(destinationRoot, sanitizedFilename)
                        Downloadable(
                            name = "${info.id}/${File(sanitizedFilename).name}",
                            source = Uri.parse(downloadUrl),
                            destination = destinationFile.absoluteFile,
                            sizeBytes = file.size.takeIf { it > 0 }
                        )
                    }
                }

                withContext(Dispatchers.Main) {
                    downloadableModels = downloadables
                    if (downloadables.isEmpty()) {
                        searchError = "No downloadable files found for \"$query\""
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to search models")
                val errorMessage = e.message ?: "Failed to search models"
                withContext(Dispatchers.Main) {
                    searchError = errorMessage
                    downloadableModels = emptyList()
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
                        source = Uri.EMPTY,
                        destination = file,
                        sizeBytes = file.length()
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
     * Enqueue a model download. Destination can be overridden per call.
     */
    fun downloadModel(downloadable: Downloadable, targetDirectory: File? = null) {
        val resolved = resolveDestination(downloadable, targetDirectory)

        viewModelScope.launch(Dispatchers.IO) {
            val alreadyQueued = queueMutex.withLock {
                val duplicate = downloadQueue.any { it.destination.absolutePath == resolved.destination.absolutePath }
                if (!duplicate) {
                    downloadQueue.add(resolved)
                    persistQueueLocked()
                }
                duplicate
            }

            if (alreadyQueued) {
                withContext(Dispatchers.Main) {
                    downloadError = "Download already queued for ${resolved.destination.name}"
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                downloadError = null
                downloadSuccess = null
            }

            processNextInQueue()
        }
    }

    /**
     * Cancel current download and clear any queued items
     */
    fun cancelDownload() {
        viewModelScope.launch(Dispatchers.IO) {
            currentDownloadJob?.cancel()
            currentDownloadJob = null
            queueMutex.withLock {
                downloadQueue.clear()
                persistQueueLocked()
            }

            withContext(Dispatchers.Main) {
                isDownloading = false
                downloadProgress = 0f
                downloadError = "Download cancelled"
                downloadSuccess = null
                currentDownloadFile = null
            }
        }
    }

    /**
     * Process next item in download queue
     */
    private fun processNextInQueue() {
        if (currentDownloadJob?.isActive == true) return

        currentDownloadJob = viewModelScope.launch(Dispatchers.IO) {
            val nextDownload = queueMutex.withLock { downloadQueue.firstOrNull() }

            if (nextDownload == null) {
                withContext(Dispatchers.Main) {
                    isDownloading = false
                    downloadProgress = 0f
                    currentDownloadFile = null
                }
                currentDownloadJob = null
                return@launch
            }

            withContext(Dispatchers.Main) {
                isDownloading = true
                downloadProgress = 0f
                downloadError = null
                downloadSuccess = null
                currentDownloadFile = nextDownload.destination.name
            }

            var downloadResult: DownloadResult? = null
            var failure: Exception? = null

            try {
                downloadResult = performDownload(nextDownload)
            } catch (e: CancellationException) {
                Timber.i("Download for ${nextDownload.destination.name} cancelled")
                throw e
            } catch (e: Exception) {
                failure = if (e is Exception) e else IOException(e)
            }

            if (downloadResult != null) {
                val message = buildSuccessMessage(nextDownload, downloadResult)
                withContext(Dispatchers.Main) {
                    downloadSuccess = message
                    downloadError = null
                    downloadProgress = 1f
                }
            } else if (failure != null) {
                val message = "Failed to download ${nextDownload.destination.name}: ${failure.message ?: "Unknown error"}"
                Timber.e(failure, message)
                withContext(Dispatchers.Main) {
                    downloadError = message
                    downloadSuccess = null
                    downloadProgress = 0f
                }
                errorHandler.reportError(failure, "Model download failed")
            }

            queueMutex.withLock {
                downloadQueue.removeAll { it.destination.absolutePath == nextDownload.destination.absolutePath }
                persistQueueLocked()
            }

            withContext(Dispatchers.Main) {
                isDownloading = false
                currentDownloadFile = null
            }

            val hasMore = queueMutex.withLock { downloadQueue.isNotEmpty() }
            currentDownloadJob = null

            if (hasMore) {
                processNextInQueue()
            }
        }
    }

    private suspend fun performDownload(downloadable: Downloadable): DownloadResult {
        val finalFile = downloadable.destination
        finalFile.parentFile?.let { parent -> if (!parent.exists()) parent.mkdirs() }

        val expectedSize = downloadable.sizeBytes?.takeIf { it > 0 }
        if (expectedSize != null && finalFile.exists() && finalFile.length() == expectedSize) {
            val checksum = computeSha256(finalFile)
            return DownloadResult(
                file = finalFile,
                bytesWritten = finalFile.length(),
                checksum = checksum,
                expectedBytes = expectedSize,
                sizeVerified = true
            )
        }

        val requestBuilder = Request.Builder().url(downloadable.source.toString())
        val token = userPreferencesRepository.huggingFaceToken
        if (token.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val tempFile = File(finalFile.parentFile, "${finalFile.name}.download")
        if (tempFile.exists()) tempFile.delete()

        try {
            okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}: ${response.message}")
                }

                val body = response.body ?: throw IOException("Empty response body")
                val responseLength = body.contentLength().takeIf { it > 0 }
                val expectedBytes = expectedSize ?: responseLength
                val digest = MessageDigest.getInstance("SHA-256")
                var downloadedBytes = 0L
                var lastProgressPercent = -1

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            downloadedBytes += read

                            if (expectedBytes != null && expectedBytes > 0) {
                                val percent = ((downloadedBytes * 100) / expectedBytes).toInt().coerceAtMost(100)
                                if (percent != lastProgressPercent) {
                                    lastProgressPercent = percent
                                    withContext(Dispatchers.Main) {
                                        downloadProgress = percent / 100f
                                    }
                                }
                            }
                        }
                        output.flush()
                    }
                }

                val checksum = digest.digest().joinToString(separator = "") { "%02x".format(it) }

                if (expectedSize != null && downloadedBytes != expectedSize) {
                    throw IOException("Size mismatch: expected $expectedSize bytes, downloaded $downloadedBytes bytes")
                }

                if (responseLength != null && downloadedBytes != responseLength) {
                    throw IOException("Incomplete download: expected $responseLength bytes, downloaded $downloadedBytes bytes")
                }

                downloadable.expectedChecksum?.takeIf { it.isNotBlank() }?.let { expectedChecksum ->
                    if (!checksum.equals(expectedChecksum, ignoreCase = true)) {
                        throw IOException("Checksum mismatch for ${finalFile.name}")
                    }
                }

                if (finalFile.exists() && !finalFile.delete()) {
                    throw IOException("Unable to replace existing file ${finalFile.absolutePath}")
                }

                if (!tempFile.renameTo(finalFile)) {
                    throw IOException("Failed to move temporary file to ${finalFile.absolutePath}")
                }

                return DownloadResult(
                    file = finalFile,
                    bytesWritten = downloadedBytes,
                    checksum = checksum,
                    expectedBytes = expectedBytes,
                    sizeVerified = expectedBytes != null && downloadedBytes == expectedBytes
                )
            }
        } catch (e: CancellationException) {
            tempFile.delete()
            throw e
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
    }

    private suspend fun persistQueueLocked() {
        userPreferencesRepository.setDownloadQueue(downloadQueue.map { it.toQueueItem() })
    }

    private suspend fun restorePersistedQueue() {
        try {
            val persisted = userPreferencesRepository.getDownloadQueue()
            if (persisted.isEmpty()) return

            val restored = persisted.mapNotNull { it.toDownloadable() }
            if (restored.isEmpty()) return

            queueMutex.withLock {
                downloadQueue.clear()
                downloadQueue.addAll(restored)
            }

            withContext(Dispatchers.Main) {
                downloadError = null
                downloadSuccess = null
            }

            Timber.d("Restored ${restored.size} queued downloads from persistence")
            processNextInQueue()
        } catch (e: Exception) {
            Timber.e(e, "Failed to restore download queue")
            errorHandler.reportError(e, "Failed to restore download queue")
        }
    }

    private fun resolveDestination(downloadable: Downloadable, targetDirectory: File?): Downloadable {
        val baseDir = targetDirectory
            ?: downloadable.destination.parentFile
            ?: appContext.getExternalFilesDir(null)
            ?: appContext.filesDir
        baseDir.mkdirs()

        val destinationFile = if (targetDirectory == null && downloadable.destination.isAbsolute) {
            downloadable.destination
        } else {
            File(baseDir, downloadable.destination.name)
        }
        destinationFile.parentFile?.let { if (!it.exists()) it.mkdirs() }

        return downloadable.copy(destination = destinationFile)
    }

    private fun buildSuccessMessage(downloadable: Downloadable, result: DownloadResult): String {
        val downloadedSize = Formatter.formatFileSize(appContext, result.bytesWritten)
        val verificationText = when {
            result.sizeVerified -> "size verified"
            result.expectedBytes != null -> "expected ${Formatter.formatFileSize(appContext, result.expectedBytes)}"
            else -> "no reference size"
        }
        val checksumNote = downloadable.expectedChecksum?.takeIf { it.isNotBlank() }?.let { " (checksum verified)" } ?: ""
        return "Downloaded ${downloadable.destination.name}: $downloadedSize ($verificationText). SHA-256: ${result.checksum}$checksumNote"
    }

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read = input.read(buffer)
            while (read != -1) {
                digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        return digest.digest().joinToString(separator = "") { "%02x".format(it) }
    }

    private fun buildDownloadUrl(modelId: String, filename: String): String {
        val encodedModelId = modelId.split('/')
            .filter { it.isNotBlank() }
            .joinToString("/") { Uri.encode(it) }
        val encodedFilename = filename.split('/')
            .filter { it.isNotBlank() }
            .joinToString("/") { Uri.encode(it) }
        return "https://huggingface.co/$encodedModelId/resolve/main/$encodedFilename?download=1"
    }

    private fun Downloadable.toQueueItem(): DownloadQueueItem =
        DownloadQueueItem(
            name = name,
            source = source.toString(),
            destination = destination.absolutePath,
            sizeBytes = sizeBytes,
            checksum = expectedChecksum
        )

    private fun DownloadQueueItem.toDownloadable(): Downloadable? {
        val uri = runCatching { Uri.parse(source) }.getOrNull() ?: return null
        val destinationFile = File(destination)
        return Downloadable(
            name = name,
            source = uri,
            destination = destinationFile,
            sizeBytes = sizeBytes,
            expectedChecksum = checksum
        )
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
        val queued = if (queueMutex.tryLock()) {
            try {
                downloadQueue.toList()
            } finally {
                queueMutex.unlock()
            }
        } else {
            downloadQueue.toList()
        }

        return mapOf(
            "isDownloading" to isDownloading,
            "currentFile" to (currentDownloadFile ?: ""),
            "progress" to downloadProgress,
            "queueSize" to queued.size,
            "queuedFiles" to queued.map { it.destination.name }
        )
    }

    private data class DownloadResult(
        val file: File,
        val bytesWritten: Long,
        val checksum: String,
        val expectedBytes: Long?,
        val sizeVerified: Boolean
    )
}
