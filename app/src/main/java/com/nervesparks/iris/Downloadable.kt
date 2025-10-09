package com.nervesparks.iris

import android.app.DownloadManager
import android.net.Uri
import timber.log.Timber
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.database.Cursor
import androidx.core.net.toUri
import com.nervesparks.iris.viewmodel.ModelViewModel

// Extension function for Cursor
private fun Cursor.getLongOrNull(columnIndex: Int): Long? {
    return if (isNull(columnIndex)) null else getLong(columnIndex)
}

data class Downloadable(val name: String, val source: Uri, val destination: File) {
    companion object {
        @JvmStatic
        private val tag: String? = this::class.qualifiedName

        sealed interface State
        data object Ready : State
        data class Downloading(val id: Long, val totalSize: Long) : State
        data class Downloaded(val downloadable: Downloadable) : State
        data class Error(val message: String) : State
        data object Stopped : State



        @Composable
        fun Button(viewModel: MainViewModel, modelViewModel: ModelViewModel, dm: DownloadManager, item: Downloadable) {

            var status: State by remember  {
                mutableStateOf(
                    when (val downloadId = getActiveDownloadId(dm, item)) {
                        null -> {

                            if (item.destination.exists() && item.destination.length() > 0 && !isPartialDownload(item.destination)) {
                                Downloaded(item)
                            } else {
                                Ready
                            }
                        }
                        else -> Downloading(downloadId, -1L)
                    }
                )
            }
            var progress by rememberSaveable  { mutableDoubleStateOf(0.0) }
            var totalSize by rememberSaveable  { mutableStateOf<Long?>(null) }

            val coroutineScope = rememberCoroutineScope()

            suspend fun waitForDownload(result: Downloading, item: Downloadable): State {
                while (true) {
                    val cursor = dm.query(DownloadManager.Query().setFilterById(result.id))

                    if (cursor == null) {
                        Timber.e("dm.query() returned null")
                        return Error("dm.query() returned null")
                    }

                    if (!cursor.moveToFirst() || cursor.count < 1) {
                        cursor.close()
                        Timber.i("cursor.moveToFirst() returned false or cursor.count < 1, download canceled?")
                        return Ready
                    }

                    val pix = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val tix = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val sofar = cursor.getLongOrNull(pix) ?: 0
                    val total = cursor.getLongOrNull(tix) ?: 1
                    totalSize = total
                    cursor.close()

                    if (sofar == total) {
                        Timber.d("Download complete: ${item.destination.path}")

                        item.destination.parentFile?.let { parentDir ->
                            withContext(Dispatchers.Main) {
                                modelViewModel.loadExistingModels(parentDir)
                            }
                        }

                        viewModel.currentDownloadable = item
                        if(viewModel.loadedModelName.value == "") {
                            viewModel.load(
                                item.destination.path,
                                userThreads = viewModel.user_thread.toInt()
                            )
                        }

                        return Downloaded(item)
                    }

                    progress = (sofar * 1.0) / total
                    delay(1000L)
                }
            }

            LaunchedEffect(status) {
                if (status is Downloading) {
                    status = waitForDownload(status as Downloading, item)
                }
            }
            fun onClick() {
                when (val s = status) {
                    is Downloaded -> {
                        viewModel.showModal = true
                        Timber.d("item.destination.path", item.destination.path.toString())
                        viewModel.currentDownloadable = item
                        // Force CPU backend for stability until Vulkan issues are resolved
                        val threadCount = maxOf(viewModel.user_thread.toInt(), 4) // Minimum 4 threads
                        val backend = "cpu" // Force CPU to avoid Vulkan crashes
                        Timber.d("Model loading parameters: path=${item.destination.path}, threads=$threadCount, backend=$backend")
                        Timber.d("Available backends: ${viewModel.availableBackends}, optimal: ${viewModel.optimalBackend}")
                        viewModel.load(item.destination.path, userThreads = threadCount, backend = backend)
                    }

                    is Downloading -> {
                        Timber.d("Downloading", "Already downloading in background")
                    }

                    else -> {
                        val request = DownloadManager.Request(item.source).apply {
                            setTitle("Downloading model")
                            setDescription("Downloading model: ${item.name}")
                            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                            setDestinationUri(item.destination.toUri())
                        }

                        val id = dm.enqueue(request)
                        status = Downloading(id, -1L) // Dynamically update status
                        coroutineScope.launch {
                            status = waitForDownload(Downloading(id, -1L), item)
                        }
                    }
                }
            }


            fun onStop() {
                if (status is Downloading) {
                    dm.remove((status as Downloading).id)
                    status = Ready
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { onClick() },
                    enabled = status !is Downloading && !viewModel.getIsSending() && viewModel.loadedModelName.value.isEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),

                ) {
                    when (status) {
                        is Downloading -> Text(
                            text = buildAnnotatedString {
                                append("Downloading ")
                                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.secondary)) {
                                    append("${(progress * 100).toInt()}%")
                                }
                            },
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        is Downloaded -> Text(
                            "Load",
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        is Ready -> Text(
                            "Download",
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        is Error -> Text(
                            "Download}",
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        is Stopped -> Text(
                            "Stopped",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }


                Spacer(Modifier.height(10.dp))

                if (status is Downloading) {
                    Button(
                        onClick = { onStop() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Stop Download", color = MaterialTheme.colorScheme.onError)
                    }
                }

                totalSize?.let {
                    Text(
                        text = "File size: ${it / (1024 * 1024)} MB",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}


fun isAlreadyDownloading(dm: DownloadManager, item: Downloadable): Boolean {
    val query = DownloadManager.Query()
        .setFilterByStatus(DownloadManager.STATUS_RUNNING or DownloadManager.STATUS_PENDING)

    val cursor = dm.query(query)

    cursor?.use {
        while (it.moveToNext()) {
            val uriIndex = it.getColumnIndex(DownloadManager.COLUMN_URI)
            val currentUri = it.getString(uriIndex)
            if (currentUri == item.source.toString()) {
                Timber.d("Item is already downloading or pending.")
                return true
            }
        }
    }
    return false
}

private fun isPartialDownload(file: File): Boolean {

    return file.name.endsWith(".partial") ||
            file.name.endsWith(".download") ||
            file.name.endsWith(".tmp") ||

            file.name.contains(".part")
}

fun getActiveDownloadId(dm: DownloadManager, item: Downloadable): Long? {
    val query = DownloadManager.Query()
        .setFilterByStatus(
            DownloadManager.STATUS_RUNNING or
                    DownloadManager.STATUS_PENDING or
                    DownloadManager.STATUS_PAUSED
        )

    dm.query(query)?.use { cursor ->
        val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_URI)
        val idIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID)

        while (cursor.moveToNext()) {
            val currentUri = cursor.getString(uriIndex)
            if (currentUri == item.source.toString()) {
                return cursor.getLong(idIndex)
            }
        }
    }
    return null
}