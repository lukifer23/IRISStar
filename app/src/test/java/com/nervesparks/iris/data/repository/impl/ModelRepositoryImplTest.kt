package com.nervesparks.iris.data.repository.impl

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import android.llama.cpp.LLamaAndroid
import com.nervesparks.iris.data.FakeUserPreferencesRepository
import com.nervesparks.iris.data.HuggingFaceApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@RunWith(RobolectricTestRunner::class)
class ModelRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var userPreferencesRepository: FakeUserPreferencesRepository
    private lateinit var apiService: HuggingFaceApiService
    private lateinit var server: MockWebServer
    private lateinit var repository: ModelRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        userPreferencesRepository = FakeUserPreferencesRepository(context)
        runTest {
            userPreferencesRepository.clearAll()
        }

        server = MockWebServer()
        server.start()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        apiService = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(HuggingFaceApiService::class.java)

        repository = ModelRepositoryImpl(
            context = context,
            llamaAndroid = LLamaAndroid(),
            huggingFaceApiService = apiService,
            userPreferencesRepository = userPreferencesRepository,
            moshi = moshi
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
        runTest {
            userPreferencesRepository.clearAll()
        }
    }

    @Test
    fun refreshAvailableModelsPrefixesBearerToken() = runBlocking {
        val token = "hf_test_token"
        userPreferencesRepository.huggingFaceToken = token

        val body = """[{"id":"model1","modelId":"Model 1","downloads":10,"likes":5,"tags":[],"siblings":[]}]"""
        server.enqueue(MockResponse().setBody(body).setResponseCode(200))

        repository.refreshAvailableModels()

        val request = server.takeRequest()
        assertEquals("Bearer $token", request.getHeader("Authorization"))
    }
}
