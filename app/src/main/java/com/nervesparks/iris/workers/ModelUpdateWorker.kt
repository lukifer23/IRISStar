package com.nervesparks.iris.workers

import android.content.Context
import timber.log.Timber
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nervesparks.iris.data.UserPreferencesRepository
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Background worker that checks for model updates and downloads new versions
 * when available.
 */
class ModelUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val client = OkHttpClient()
    private val moshi = Moshi.Builder().build()
    private val prefs = UserPreferencesRepository.getInstance(appContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelsDir = applicationContext.getExternalFilesDir(null)
            ?: return@withContext Result.failure()

        val cachedJson = prefs.getCachedModels()
        if (cachedJson.isBlank()) return@withContext Result.success()

        val listType = Types.newParameterizedType(List::class.java, CachedModel::class.java)
        val adapter = moshi.adapter<List<CachedModel>>(listType)
        val models = adapter.fromJson(cachedJson) ?: emptyList()

        var allSuccess = true

        for (model in models) {
            val dest = File(modelsDir, model.destination)
            if (!dest.exists() || model.source == "local") continue

            try {
                val headReq = Request.Builder().url(model.source).head().build()
                client.newCall(headReq).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val remoteSize = resp.header("Content-Length")?.toLong() ?: -1L
                    val remoteHash = resp.header("ETag")?.replace("\"", "")
                    val localHash = sha256(dest)
                    val needsUpdate =
                        (remoteSize > 0 && remoteSize != dest.length()) ||
                        (remoteHash != null && remoteHash != localHash)
                    if (needsUpdate) {
                        downloadFile(model.source, dest)
                    }
                }
            } catch (e: Exception) {
                Timber.tag("ModelUpdateWorker").e(e, "Error updating ${model.name}")
                allSuccess = false
            }
        }

        if (allSuccess) Result.success() else Result.failure()
    }

    private suspend fun downloadFile(url: String, dest: File) {
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw RuntimeException("HTTP ${response.code}")
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(dest).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    private fun sha256(file: File): String {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            var read = input.read(buffer)
            while (read > 0) {
                digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        return digest.digest().joinToString(separator = "") { b -> "%02x".format(b) }
    }

    @JsonClass(generateAdapter = true)
    data class CachedModel(
        val name: String,
        val source: String,
        val destination: String,
        val supportsReasoning: Boolean = false
    )

    companion object {
        private const val WORK_NAME = "model_update"

        /** Schedule periodic model update checks. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ModelUpdateWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

