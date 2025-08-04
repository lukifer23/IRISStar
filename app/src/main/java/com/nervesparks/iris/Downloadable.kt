package com.nervesparks.iris

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.nervesparks.iris.data.repository.DownloadState
import java.io.File

/**
 * Represents a model that can be downloaded.
 */
data class Downloadable(val name: String, val source: Uri, val destination: File) {
    companion object {
        /**
         * Simple download button that delegates work to [MainViewModel].
         */
        @Composable
        fun Button(viewModel: MainViewModel, item: Downloadable) {
            val state by viewModel.downloadState.collectAsState(initial = null)
            val progress = (state as? DownloadState.Progress)?.takeIf { it.item == item }
            val completed = (state as? DownloadState.Completed)?.item == item || item.destination.exists()

            Button(
                onClick = { viewModel.downloadModel(item) },
                enabled = !completed && progress == null,
            ) {
                when {
                    completed -> Text("Downloaded")
                    progress != null -> {
                        LinearProgressIndicator(
                            progress = if (progress.totalBytes > 0) progress.bytesDownloaded / progress.totalBytes.toFloat() else 0f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> Text("Download")
                }
            }
        }
    }
}
