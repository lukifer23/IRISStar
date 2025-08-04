package com.nervesparks.iris.data.repository.impl

import com.nervesparks.iris.Downloadable
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject

/**
 * Basic [Downloader] implementation backed by OkHttp.
 * This is intentionally simple and meant primarily for development usage.
 */
class OkHttpDownloader @Inject constructor(
    private val client: OkHttpClient = OkHttpClient()
) : Downloader {
    override suspend fun download(item: Downloadable, onProgress: suspend (Long, Long) -> Unit) {
        val request = Request.Builder().url(item.source.toString()).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${'$'}{response.code}")
            val body = response.body ?: throw IOException("empty body")
            val total = body.contentLength().takeIf { it > 0 } ?: -1
            item.destination.outputStream().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytes = input.read(buffer)
                    var downloaded = 0L
                    while (bytes >= 0) {
                        output.write(buffer, 0, bytes)
                        downloaded += bytes
                        if (total > 0) onProgress(downloaded, total)
                        bytes = input.read(buffer)
                    }
                }
            }
        }
    }
}
