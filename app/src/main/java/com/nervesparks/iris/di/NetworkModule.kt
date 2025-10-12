package com.nervesparks.iris.di

import android.content.Context
import com.nervesparks.iris.data.AndroidSearchService
import com.nervesparks.iris.data.DocumentRepository
import com.nervesparks.iris.data.HuggingFaceApiService
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.data.WebSearchService
import com.nervesparks.iris.data.network.CacheControlInterceptor
import com.nervesparks.iris.data.network.NetworkConfig
import com.nervesparks.iris.data.network.RequestDeduplicationInterceptor
import com.nervesparks.iris.data.network.CertificatePins
import com.nervesparks.iris.llm.EmbeddingService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideNetworkConfig(): NetworkConfig = NetworkConfig()

    @Provides
    @Singleton
    fun provideRequestDeduplicationInterceptor(): RequestDeduplicationInterceptor =
        RequestDeduplicationInterceptor()

    @Provides
    @Singleton
    fun provideCacheControlInterceptor(config: NetworkConfig): CacheControlInterceptor =
        CacheControlInterceptor(config)

    @Provides
    @Singleton
    fun provideCertificatePinner(): CertificatePinner = CertificatePins.build()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        config: NetworkConfig,
        dedupe: RequestDeduplicationInterceptor,
        cacheControlInterceptor: CacheControlInterceptor,
        certificatePinner: CertificatePinner
    ): OkHttpClient {
        val cacheDir = File(context.cacheDir, "http_cache")
        val cache = Cache(cacheDir, config.cacheSizeBytes)

        return OkHttpClient.Builder()
            .cache(cache)
            .certificatePinner(certificatePinner)
            .addInterceptor(dedupe)
            .addNetworkInterceptor(cacheControlInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(moshi: Moshi, client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://huggingface.co/api/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideHuggingFaceApiService(retrofit: Retrofit): HuggingFaceApiService {
        return retrofit.create(HuggingFaceApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideWebSearchService(
        client: OkHttpClient,
        userPreferencesRepository: UserPreferencesRepository
    ): WebSearchService = WebSearchService(client, userPreferencesRepository)

    @Provides
    @Singleton
    fun provideAndroidSearchService(
        @ApplicationContext context: Context,
        documentRepository: DocumentRepository,
        embeddingService: EmbeddingService
    ): AndroidSearchService = AndroidSearchService(context, documentRepository, embeddingService)
}
