package com.nervesparks.iris.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class HuggingFaceApiServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var api: HuggingFaceApiService

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(HuggingFaceApiService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun searchModelsSendsAuthHeader() {
        val body = """[{"id":"model1","modelId":"Model 1","downloads":10,"likes":5,"tags":[]}]"""
        server.enqueue(MockResponse().setBody(body).setResponseCode(200))

        runBlocking {
            val result = api.searchModels("test", token = "Bearer abc")
            val request = server.takeRequest()
            assertEquals("/models?search=test", request.path)
            assertEquals("Bearer abc", request.getHeader("Authorization"))
            assertEquals(1, result.size)
            assertEquals("model1", result[0].id)
        }
    }
}

