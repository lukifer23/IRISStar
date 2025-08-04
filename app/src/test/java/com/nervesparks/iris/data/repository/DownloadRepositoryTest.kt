package com.nervesparks.iris.data.repository

import android.net.Uri
import com.nervesparks.iris.Downloadable
import com.nervesparks.iris.data.repository.impl.DownloadRepositoryImpl
import com.nervesparks.iris.data.repository.impl.Downloader
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DownloadRepositoryTest {

    @Test
    fun successfulDownloadEmitsCompletion() = runTest {
        val fake = object : Downloader {
            override suspend fun download(item: Downloadable, onProgress: suspend (Long, Long) -> Unit) {
                onProgress(50, 100)
                onProgress(100, 100)
            }
        }
        val repo = DownloadRepositoryImpl(fake, this)
        val item = Downloadable("test", Uri.parse("http://example.com"), File.createTempFile("test",".tmp"))
        repo.enqueue(item)
        val state = repo.downloadState.first { it is DownloadState.Completed }
        assertTrue(state is DownloadState.Completed && state.item == item)
    }

    @Test
    fun failedDownloadEmitsFailed() = runTest {
        val fake = object : Downloader {
            override suspend fun download(item: Downloadable, onProgress: suspend (Long, Long) -> Unit) {
                throw RuntimeException("boom")
            }
        }
        val repo = DownloadRepositoryImpl(fake, this)
        val item = Downloadable("test", Uri.parse("http://example.com"), File.createTempFile("test",".tmp"))
        repo.enqueue(item)
        val state = repo.downloadState.first { it is DownloadState.Failed }
        assertTrue(state is DownloadState.Failed)
    }

    @Test
    fun canceledDownloadEmitsCanceled() = runTest {
        val fake = object : Downloader {
            override suspend fun download(item: Downloadable, onProgress: suspend (Long, Long) -> Unit) {
                repeat(5) {
                    onProgress((it + 1) * 10L, 100)
                    delay(10)
                }
            }
        }
        val repo = DownloadRepositoryImpl(fake, this)
        val item = Downloadable("test", Uri.parse("http://example.com"), File.createTempFile("test",".tmp"))
        val id = repo.enqueue(item)
        launch { delay(20); repo.cancel(id) }
        val state = repo.downloadState.first { it is DownloadState.Canceled }
        assertTrue(state is DownloadState.Canceled)
    }
}
