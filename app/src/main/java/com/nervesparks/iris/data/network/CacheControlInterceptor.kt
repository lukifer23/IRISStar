package com.nervesparks.iris.data.network

import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Applies caching headers according to [NetworkConfig].
 */
class CacheControlInterceptor(
    private val config: NetworkConfig
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val cacheControl = CacheControl.Builder()
            .maxAge(config.maxAgeSeconds, TimeUnit.SECONDS)
            .maxStale(config.maxStaleSeconds, TimeUnit.SECONDS)
            .build()

        return response.newBuilder()
            .header("Cache-Control", cacheControl.toString())
            .build()
    }
}
