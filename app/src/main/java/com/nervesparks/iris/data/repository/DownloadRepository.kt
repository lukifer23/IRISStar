package com.nervesparks.iris.data.repository

import com.nervesparks.iris.Downloadable
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Repository for handling model downloads.
 */
interface DownloadRepository {
    /**
     * Flow emitting download state updates.
     */
    val downloadState: Flow<DownloadState>

    /**
     * Enqueue a download for the given item.
     * @return download identifier.
     */
    fun enqueue(item: Downloadable): Long

    /**
     * Cancel an active download.
     */
    fun cancel(id: Long)
}

/**
 * Represents download progress and terminal states.
 */
sealed class DownloadState {
    abstract val id: Long
    abstract val item: Downloadable

    data class Progress(
        override val id: Long,
        override val item: Downloadable,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : DownloadState()

    data class Completed(
        override val id: Long,
        override val item: Downloadable
    ) : DownloadState()

    data class Failed(
        override val id: Long,
        override val item: Downloadable,
        val throwable: Throwable
    ) : DownloadState()

    data class Canceled(
        override val id: Long,
        override val item: Downloadable
    ) : DownloadState()
}
