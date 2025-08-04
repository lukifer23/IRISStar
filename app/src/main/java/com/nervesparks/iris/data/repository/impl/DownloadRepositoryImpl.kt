package com.nervesparks.iris.data.repository.impl

import com.nervesparks.iris.Downloadable
import com.nervesparks.iris.data.repository.DownloadRepository
import com.nervesparks.iris.data.repository.DownloadState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

/**
 * Default implementation of [DownloadRepository].
 * Uses a provided [Downloader] to perform the actual download work and
 * emits progress updates and terminal states via a shared flow.
 */
class DownloadRepositoryImpl @Inject constructor(
    private val downloader: Downloader,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : DownloadRepository {

    private val nextId = AtomicLong(0)
    private val jobs = ConcurrentHashMap<Long, Job>()
    private val _state = MutableSharedFlow<DownloadState>(extraBufferCapacity = 16)
    override val downloadState = _state.asSharedFlow()

    override fun enqueue(item: Downloadable): Long {
        val id = nextId.incrementAndGet()
        val job = scope.launch {
            try {
                downloader.download(item) { bytes, total ->
                    _state.emit(DownloadState.Progress(id, item, bytes, total))
                }
                _state.emit(DownloadState.Completed(id, item))
            } catch (e: CancellationException) {
                _state.emit(DownloadState.Canceled(id, item))
            } catch (t: Throwable) {
                _state.emit(DownloadState.Failed(id, item, t))
            }
        }
        jobs[id] = job
        return id
    }

    override fun cancel(id: Long) {
        jobs[id]?.cancel()
    }
}

/**
 * Simple downloader abstraction for easier testing.
 */
fun interface Downloader {
    suspend fun download(item: Downloadable, onProgress: suspend (bytes: Long, total: Long) -> Unit)
}
