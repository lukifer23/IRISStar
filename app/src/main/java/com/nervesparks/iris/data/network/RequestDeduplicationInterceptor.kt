package com.nervesparks.iris.data.network

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.concurrent.ConcurrentHashMap

/**
 * Interceptor that avoids executing identical requests concurrently.
 * Requests with the same method and URL share a single network call.
 */
class RequestDeduplicationInterceptor : Interceptor {

    private data class CachedResponse(
        val code: Int,
        val message: String,
        val headers: Headers,
        val protocol: Protocol,
        val bodyBytes: ByteArray,
        val mediaType: MediaType?
    )

    private val inFlight = ConcurrentHashMap<String, CompletableDeferred<CachedResponse>>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val key = "${request.method}:${request.url}"

        val newDeferred = CompletableDeferred<CachedResponse>()
        val existing = inFlight.putIfAbsent(key, newDeferred)
        val deferred = existing ?: newDeferred

        if (existing != null) {
            val cached = runBlocking { deferred.await() }
            return Response.Builder()
                .request(request)
                .protocol(cached.protocol)
                .code(cached.code)
                .message(cached.message)
                .headers(cached.headers)
                .body(cached.bodyBytes.toResponseBody(cached.mediaType))
                .build()
        }

        return try {
            val response = chain.proceed(request)
            val bytes = response.body?.bytes() ?: ByteArray(0)
            val mediaType = response.body?.contentType()
            val cached = CachedResponse(
                response.code,
                response.message,
                response.headers,
                response.protocol,
                bytes,
                mediaType
            )
            newDeferred.complete(cached)
            response.newBuilder().body(bytes.toResponseBody(mediaType)).build()
        } catch (e: Exception) {
            newDeferred.completeExceptionally(e)
            throw e
        } finally {
            inFlight.remove(key)
        }
    }
}
